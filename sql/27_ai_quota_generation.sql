-- ============================================================
-- 对话框多能力 P5+P3：配额 / 按人用量 / 生成任务 三表 + 种子（MySQL 8.0+）
-- 关联方案：《对话框多能力补全与图像视频生成实现方案》P5、P3
-- 背景：
--   ai_quota_rule   —— 按「user_type + task_type」限日/月次数（0=不限），只存阈值不存用量；
--   ai_usage_record —— 按「user_id + user_type + 日 + task_type」原子聚合用量，配额判定与「按人可归集」计数来源；
--                       与 agent_token_usage（租户/智能体维度）并存不双写同一列；
--   ai_generation_task —— 图像/视频异步生成任务状态机（提交→轮询→转存 infra_file），供应商 URL 24h 过期须转存。
-- 口径：聚合行只增不删（is_delete 参与唯一键，删过的行会与新行撞键）；配额判定读 request_count，
--       image_count/video_count 为生成成功数（成本归因，含 R3 转存失败仍计入）。
-- 幂等：菜单先删后插；配额规则先删后插；授权显式落库。
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1. AI 配额规则表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `ai_quota_rule`;
CREATE TABLE `ai_quota_rule` (
    `id`            BIGINT       NOT NULL COMMENT '主键ID（雪花算法）',
    `user_type`     VARCHAR(16)  NOT NULL DEFAULT 'member' COMMENT '用户类型：admin-管理端 member-会员端',
    `task_type`     VARCHAR(16)  NOT NULL DEFAULT 'chat' COMMENT '任务类型：chat/copywriting/docx/pdf/parse/image/video',
    `daily_limit`   INT          NOT NULL DEFAULT 0 COMMENT '每日次数上限，0=不限',
    `monthly_limit` INT          NOT NULL DEFAULT 0 COMMENT '每月次数上限，0=不限',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-停用（不校验） 1-启用',
    `remark`        VARCHAR(255) NOT NULL DEFAULT '' COMMENT '备注',
    `create_by`     BIGINT           NULL COMMENT '创建人ID',
    `create_time`   DATETIME         NULL COMMENT '创建时间',
    `update_by`     BIGINT           NULL COMMENT '更新人ID',
    `update_time`   DATETIME         NULL COMMENT '更新时间',
    `is_delete`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_task` (`user_type`, `task_type`, `is_delete`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 配额规则表';

-- ------------------------------------------------------------
-- 2. AI 按人按日用量表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `ai_usage_record`;
CREATE TABLE `ai_usage_record` (
    `id`            BIGINT         NOT NULL COMMENT '主键ID（雪花算法）',
    `usage_date`    DATE           NOT NULL COMMENT '统计日期',
    `user_id`       BIGINT         NOT NULL COMMENT '用户ID',
    `user_type`     VARCHAR(16)    NOT NULL COMMENT '用户类型：admin-管理端 member-会员端',
    `task_type`     VARCHAR(16)    NOT NULL DEFAULT 'chat' COMMENT '任务类型',
    `request_count` INT            NOT NULL DEFAULT 0 COMMENT '请求次数（配额判定依据）',
    `image_count`   INT            NOT NULL DEFAULT 0 COMMENT '图像生成数（含转存失败，成本归因）',
    `video_count`   INT            NOT NULL DEFAULT 0 COMMENT '视频生成数（含转存失败，成本归因）',
    `input_tokens`  BIGINT         NOT NULL DEFAULT 0 COMMENT '输入 Token 累计',
    `output_tokens` BIGINT         NOT NULL DEFAULT 0 COMMENT '输出 Token 累计',
    `total_tokens`  BIGINT         NOT NULL DEFAULT 0 COMMENT '总 Token 累计',
    `cost`          DECIMAL(14, 6) NOT NULL DEFAULT 0 COMMENT '预估成本（按模型单价折算）',
    `create_by`     BIGINT             NULL COMMENT '创建人ID',
    `create_time`   DATETIME           NULL COMMENT '创建时间',
    `update_by`     BIGINT             NULL COMMENT '更新人ID',
    `update_time`   DATETIME           NULL COMMENT '更新时间',
    `is_delete`     TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_day_task` (`user_id`, `user_type`, `usage_date`, `task_type`, `is_delete`),
    KEY `idx_user_type_date` (`user_type`, `usage_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 按人按日用量表';

-- ------------------------------------------------------------
-- 3. AI 生成任务表（图像/视频）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `ai_generation_task`;
CREATE TABLE `ai_generation_task` (
    `id`             BIGINT        NOT NULL COMMENT '主键ID（雪花算法）',
    `task_id`        VARCHAR(64)   NOT NULL COMMENT '对外任务号（UUID，前端轮询凭据）',
    `remote_task_id` VARCHAR(128)  NOT NULL DEFAULT '' COMMENT '供应商任务号',
    `provider`       VARCHAR(32)   NOT NULL DEFAULT 'dashscope' COMMENT '供应商标识',
    `task_type`      VARCHAR(16)   NOT NULL COMMENT '任务类型：image/video',
    `model`          VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '实际模型名',
    `prompt`         TEXT          NOT NULL COMMENT '生成提示词',
    `options`        JSON          NULL COMMENT '生成参数快照（size/n/duration 等）',
    `session_id`     VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '来源会话ID（产物回填对话）',
    `message_id`     BIGINT        NOT NULL DEFAULT 0 COMMENT '来源消息ID，0=不回填',
    `user_id`        BIGINT        NOT NULL COMMENT '归属用户ID',
    `user_type`      VARCHAR(16)   NOT NULL COMMENT '用户类型：admin/member',
    `tenant_id`      BIGINT        NOT NULL DEFAULT 0 COMMENT '租户ID',
    `status`         TINYINT       NOT NULL DEFAULT 0 COMMENT '0排队 1运行 2成功 3失败 4超时 5待审批',
    `file_id`        BIGINT        NOT NULL DEFAULT 0 COMMENT 'infra_file.id，0=未转存',
    `result_url`     VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '供应商产物URL（原始，24h过期）',
    `file_size`      BIGINT        NOT NULL DEFAULT 0 COMMENT '转存后字节大小',
    `error_msg`      VARCHAR(512)  NOT NULL DEFAULT '' COMMENT '失败原因',
    `poll_count`     INT           NOT NULL DEFAULT 0 COMMENT '轮询次数（兜底接管判据）',
    `submit_time`    DATETIME      NOT NULL COMMENT '提交时间',
    `finish_time`    DATETIME          NULL COMMENT '结束时间',
    `create_by`      BIGINT            NULL COMMENT '创建人ID',
    `create_time`    DATETIME          NULL COMMENT '创建时间',
    `update_by`      BIGINT            NULL COMMENT '更新人ID',
    `update_time`    DATETIME          NULL COMMENT '更新时间',
    `is_delete`      TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_id` (`task_id`, `is_delete`),
    KEY `idx_status_submit` (`status`, `submit_time`),
    KEY `idx_user` (`user_id`, `user_type`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 生成任务表（图像/视频）';

-- ------------------------------------------------------------
-- 4. 配额规则种子：member 分级限次，admin 全 0（不限，代码亦直接豁免）
-- ------------------------------------------------------------
DELETE FROM `ai_quota_rule` WHERE `id` BETWEEN 95001 AND 95099;
INSERT INTO `ai_quota_rule` (`id`, `user_type`, `task_type`, `daily_limit`, `monthly_limit`, `status`, `remark`) VALUES
    (95001, 'member', 'chat',        100,  0,  1, '会员对话：每日100次'),
    (95002, 'member', 'copywriting', 50,   0,  1, '会员文案：每日50次'),
    (95003, 'member', 'docx',        50,   0,  1, '会员Word导出：每日50次'),
    (95004, 'member', 'pdf',         50,   0,  1, '会员PDF导出：每日50次'),
    (95005, 'member', 'parse',       50,   0,  1, '会员附件解析：每日50次'),
    (95006, 'member', 'image',       10,   0,  1, '会员图像生成：每日10次'),
    (95007, 'member', 'video',       2,    20, 1, '会员视频生成：每日2次/每月20次'),
    (95011, 'admin',  'image',       0,    0,  1, '管理端图像：不限'),
    (95012, 'admin',  'video',       0,    0,  1, '管理端视频：不限');

-- ------------------------------------------------------------
-- 5. 菜单权限：配额管理(3117) + 生成任务(3118)，挂智能体管理目录 3010
-- ------------------------------------------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (3117, 31171, 31172, 31173, 3118);
DELETE FROM `sys_menu` WHERE `id` IN (3117, 31171, 31172, 31173, 3118);

-- 配额管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `component`, `sort`, `visible`, `status`)
VALUES (3117, 3010, '配额管理', 2, 'agent:quota:list', '/agent/quota', '/agent/quota/index', 8, 1, 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (31171, 3117, '配额新增', 3, 'agent:quota:create', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (31172, 3117, '配额修改', 3, 'agent:quota:update', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (31173, 3117, '配额删除', 3, 'agent:quota:delete', 3);

-- 生成任务（只读列表）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `component`, `sort`, `visible`, `status`)
VALUES (3118, 3010, '生成任务', 2, 'agent:task:list', '/agent/task', '/agent/task/index', 9, 1, 1);

-- 授权给超级管理员（role_id=1）
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES
    (99024, 1, 3117), (99025, 1, 31171), (99026, 1, 31172), (99027, 1, 31173), (99028, 1, 3118);

-- ------------------------------------------------------------
-- 6. ai-chat 智能体挂载图像/视频生成工具（P3/P4）
--    tool_names 追加 generateImage/generateVideo，并把 max_steps 由 8 提到 10（异步提交 + 提示话术余量）。
--    注意：工具是否真正生效取决于运行配置 zhush.agentscope.generation.enabled（默认 false）：
--      - false：模型即便调用工具，工具也会即时回「生成能力未开启」并提示联系管理员，不产生脏任务；
--      - true ：需已在 ai_model 配置启用态 image/video 模型（见 28_ai_generation_model.sql，默认停用）。
--    因此此处提前注册工具不影响未开启生成的环境，仅降低后续上线的变更成本（幂等，重复执行安全）。
-- ------------------------------------------------------------
UPDATE `agent_definition`
SET `tool_names`    = '["generateDocument", "parseDocument", "generateImage", "generateVideo"]',
    `system_prompt` = '你是一位专业的智能助手，请友好、准确地回答用户问题。\n\n【能力与工具约定】\n1. 生成文档：当用户要求导出/生成 Word、PDF 或下载文档时，先产出结构完整的 Markdown 正文（标题/段落/列表/表格），再调用 generateDocument 工具（content=正文，format=docx|pdf，缺省 docx），将返回结果中 artifact 的链接以 Markdown 文件链接形式附在回复末尾，勿自行编造下载地址。\n2. 解析附件：当用户消息含【附件】name: url 时，如需了解文件内容调用 parseDocument（fileUrl=url）；未上传附件时不要调用。\n3. 生成图片：当用户要求画图/生成图片时，调用 generateImage（prompt=描述）；任务为异步，提交后只需告知用户“图片正在生成，请稍候”，切勿编造图片链接。\n4. 生成视频：当用户要求生成视频时，调用 generateVideo（prompt=描述）；任务异步且耗时，告知用户“视频正在生成，可能需要数分钟”，切勿编造视频链接。\n5. 选工具还是直答由你按意图判断；与工具无关的请求不要调用工具。',
    `max_steps`     = 10,
    `version`       = `version` + 1
WHERE `id` = 90001 AND `name` = 'ai-chat';

-- ------------------------------------------------------------
-- 7. 校验
-- ------------------------------------------------------------
-- SELECT COUNT(*) FROM ai_quota_rule;                 -- 期望 9
-- SELECT id, user_type, task_type, daily_limit FROM ai_quota_rule ORDER BY id;
-- SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
--   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('ai_quota_rule','ai_usage_record','ai_generation_task');
-- SELECT id, name, permission FROM sys_menu WHERE id IN (3117,3118);
-- SELECT tool_names, max_steps, version FROM agent_definition WHERE id = 90001;
