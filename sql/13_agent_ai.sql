-- ============================================================
-- AI 智能体模块数据表 · 建表脚本
-- 关联方案：《AgentScope 2.0 整合方案》 + 《AI智能体前端页面设计方案》
-- 字符集：utf8mb4 / utf8mb4_0900_ai_ci；主键统一雪花算法。
-- 多租户：公共业务表均含 tenant_id，贯穿会话/工具/审计，保证租户隔离。
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 1. 智能体定义表：管理端维护 Agent（提示词/模型/工具/步数/分组）
-- ============================================================
DROP TABLE IF EXISTS `agent_definition`;
CREATE TABLE `agent_definition` (
    `id`            BIGINT       NOT NULL COMMENT '主键ID（雪花算法）',
    `name`          VARCHAR(64)  NOT NULL COMMENT '智能体唯一标识（如 union-selector、ai-chat）',
    `description`   VARCHAR(256) NOT NULL DEFAULT '' COMMENT '功能说明',
    `system_prompt` TEXT         NOT NULL COMMENT '系统提示词',
    `model`         VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '模型名（如 qwen-max，空则用默认模型）',
    `tool_names`    JSON         NULL COMMENT '允许使用的工具清单（JSON 数组）',
    `streaming`     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否支持流式：0-否 1-是',
    `max_steps`     INT          NOT NULL DEFAULT 6 COMMENT '最大推理步数',
    `skills`        JSON         NULL COMMENT '技能清单（HarnessAgent Skill，JSON 数组）',
    `group_code`    VARCHAR(32)  NOT NULL DEFAULT 'common' COMMENT '分组：common-公共 biz-业务（用于前端分组展示）',
    `source`        TINYINT      NOT NULL DEFAULT 1 COMMENT '来源：1-代码声明(AgentDefinitionProvider) 2-管理端配置(落库) 3-运行时动态注册',
    `version`       INT          NOT NULL DEFAULT 1 COMMENT '版本号（提示词/配置热更时自增）',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-停用 1-启用',
    `remark`        VARCHAR(512) NOT NULL DEFAULT '' COMMENT '备注',
    `create_by`     BIGINT           NULL COMMENT '创建人ID',
    `create_time`   DATETIME         NULL COMMENT '创建时间',
    `update_by`     BIGINT           NULL COMMENT '更新人ID',
    `update_time`   DATETIME         NULL COMMENT '更新时间',
    `is_delete`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`, `is_delete`),
    KEY `idx_group_code` (`group_code`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '智能体定义表';

-- ============================================================
-- 2. 智能体工具注册表：@AgentTool 注解扫描快照（只读展示）
-- ============================================================
DROP TABLE IF EXISTS `agent_tool`;
CREATE TABLE `agent_tool` (
    `id`                BIGINT       NOT NULL COMMENT '主键ID（雪花算法）',
    `name`              VARCHAR(64)  NOT NULL COMMENT '工具名（如 queryHotGoods）',
    `description`       VARCHAR(512) NOT NULL DEFAULT '' COMMENT '工具说明（给 LLM 看）',
    `bean_name`         VARCHAR(128) NOT NULL DEFAULT '' COMMENT '所属 Spring Bean 名',
    `method_name`       VARCHAR(128) NOT NULL DEFAULT '' COMMENT '所属方法名',
    `requires_approval` TINYINT      NOT NULL DEFAULT 0 COMMENT '是否需人工审批：0-否 1-是',
    `group_code`        VARCHAR(32)  NOT NULL DEFAULT 'common' COMMENT '所属业务分组',
    `params_doc`        JSON         NULL COMMENT '入参说明（JSON，供前端展示参数结构）',
    `status`            TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-停用 1-启用',
    `reload_time`       DATETIME         NULL COMMENT '注册/重新扫描时间',
    `create_by`         BIGINT           NULL COMMENT '创建人ID',
    `create_time`       DATETIME         NULL COMMENT '创建时间',
    `update_by`         BIGINT           NULL COMMENT '更新人ID',
    `update_time`       DATETIME         NULL COMMENT '更新时间',
    `is_delete`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`, `is_delete`),
    KEY `idx_group_code` (`group_code`),
    KEY `idx_requires_approval` (`requires_approval`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '智能体工具注册表';

-- ============================================================
-- 3. AI 对话会话表：会话记录（列表查询、统计、恢复入口）
-- ============================================================
DROP TABLE IF EXISTS `ai_chat_session`;
CREATE TABLE `ai_chat_session` (
    `id`                BIGINT       NOT NULL COMMENT '主键ID（雪花算法）',
    `session_id`        VARCHAR(64)  NOT NULL COMMENT '业务会话ID（AgentSessionManager 生成，全局唯一）',
    `agent_name`        VARCHAR(64)  NOT NULL COMMENT '智能体名',
    `agent_desc`        VARCHAR(256) NOT NULL DEFAULT '' COMMENT '智能体描述快照',
    `model`             VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '模型名快照',
    `title`             VARCHAR(128) NOT NULL DEFAULT '' COMMENT '会话标题（取首条用户消息截断）',
    `user_id`           BIGINT       NOT NULL COMMENT '发起用户ID',
    `user_type`         VARCHAR(16)  NOT NULL DEFAULT 'admin' COMMENT '用户类型：admin-管理端 member-会员端',
    `tenant_id`         BIGINT       NOT NULL DEFAULT 0 COMMENT '租户ID（多租户隔离）',
    `message_count`     INT          NOT NULL DEFAULT 0 COMMENT '消息数',
    `token_cost`        BIGINT       NOT NULL DEFAULT 0 COMMENT '累计 Token 消耗',
    `status`            TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-进行中 2-已结束 3-审批挂起 4-异常',
    `start_time`        DATETIME         NULL COMMENT '会话开始时间',
    `finish_time`       DATETIME         NULL COMMENT '会话结束时间',
    `last_active_time`  DATETIME         NULL COMMENT '最近交互时间（列表排序）',
    `create_by`         BIGINT           NULL COMMENT '创建人ID',
    `create_time`       DATETIME         NULL COMMENT '创建时间',
    `update_by`         BIGINT           NULL COMMENT '更新人ID',
    `update_time`       DATETIME         NULL COMMENT '更新时间',
    `is_delete`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_session_id` (`session_id`, `is_delete`),
    KEY `idx_agent_name` (`agent_name`),
    KEY `idx_user` (`user_type`, `user_id`),
    KEY `idx_tenant_last_active` (`tenant_id`, `last_active_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 对话会话表';

-- ============================================================
-- 4. AI 对话消息表：逐条消息（含思考/工具调用/Token 用量）
-- ============================================================
DROP TABLE IF EXISTS `ai_chat_message`;
CREATE TABLE `ai_chat_message` (
    `id`            BIGINT       NOT NULL COMMENT '主键ID（雪花算法）',
    `session_id`    VARCHAR(64)  NOT NULL COMMENT '业务会话ID（关联 ai_chat_session.session_id）',
    `seq`           INT          NOT NULL COMMENT '会话内消息序号（自增可恢复顺序）',
    `role`          VARCHAR(16)  NOT NULL COMMENT '角色：user 用户 assistant 助手 tool 工具',
    `content_type`  VARCHAR(16)  NOT NULL DEFAULT 'text' COMMENT '内容类型：text 纯文本 markdown',
    `content`       TEXT         NOT NULL COMMENT '消息正文',
    `thinking`      TEXT         NULL COMMENT '思考过程（assistant，流式累积）',
    `tool_calls`    JSON         NULL COMMENT '工具调用（assistant，JSON 数组：name/arguments/result/status）',
    `token_usage`   JSON         NULL COMMENT 'Token 用量（JSON：input/output/total）',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-正常 2-流式中 3-已中断 4-失败',
    `error_msg`     VARCHAR(512) NOT NULL DEFAULT '' COMMENT '失败/中断原因',
    `create_by`     BIGINT           NULL COMMENT '创建人ID',
    `create_time`   DATETIME         NULL COMMENT '创建时间',
    `update_by`     BIGINT           NULL COMMENT '更新人ID',
    `update_time`   DATETIME         NULL COMMENT '更新时间',
    `is_delete`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_session_seq` (`session_id`, `seq`, `is_delete`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 对话消息表';

-- ============================================================
-- 5. 智能体审批任务表：敏感工具人工审批（通过/改参/驳回）
-- ============================================================
DROP TABLE IF EXISTS `agent_approval`;
CREATE TABLE `agent_approval` (
    `id`              BIGINT       NOT NULL COMMENT '主键ID（雪花算法）',
    `approval_id`     VARCHAR(64)  NOT NULL COMMENT '审批编号（Redis key：agentscope:approval:{sessionId}:{tool} 的后缀）',
    `session_id`      VARCHAR(64)  NOT NULL COMMENT '业务会话ID',
    `agent_name`      VARCHAR(64)  NOT NULL COMMENT '智能体名',
    `tool_name`       VARCHAR(64)  NOT NULL COMMENT '敏感工具名',
    `tool_args`       TEXT         NOT NULL COMMENT '原始入参（JSON）',
    `modified_args`   TEXT         NULL COMMENT '改参通过后的入参（JSON，改参时写入）',
    `proposer_id`     BIGINT       NOT NULL COMMENT '提交人用户ID',
    `proposer_type`   VARCHAR(16)  NOT NULL DEFAULT 'admin' COMMENT '提交人类型：admin-管理端 member-会员端',
    `proposer_name`   VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '提交人名称',
    `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0-待审批 1-已通过 2-已驳回 3-已过期',
    `approve_user_id` BIGINT           NULL COMMENT '审批人用户ID',
    `approve_time`    DATETIME         NULL COMMENT '审批时间',
    `reject_reason`   VARCHAR(512) NOT NULL DEFAULT '' COMMENT '驳回原因',
    `expire_time`     DATETIME         NULL COMMENT '过期时间（对齐 Redis TTL）',
    `create_by`       BIGINT           NULL COMMENT '创建人ID',
    `create_time`     DATETIME         NULL COMMENT '创建时间',
    `update_by`       BIGINT           NULL COMMENT '更新人ID',
    `update_time`     DATETIME         NULL COMMENT '更新时间',
    `is_delete`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_approval_id` (`approval_id`, `is_delete`),
    KEY `idx_status` (`status`),
    KEY `idx_tool_name` (`tool_name`),
    KEY `idx_session_id` (`session_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '智能体审批任务表';

-- ============================================================
-- 6. AI 审计日志表：事件总线落库（会话/工具/错误/审批）
-- ============================================================
DROP TABLE IF EXISTS `agent_audit_log`;
CREATE TABLE `agent_audit_log` (
    `id`            BIGINT       NOT NULL COMMENT '主键ID（雪花算法）',
    `trace_id`      VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '链路追踪ID（SkyWalking TID）',
    `session_id`    VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '业务会话ID',
    `agent_name`    VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '智能体名',
    `event_type`    VARCHAR(32)  NOT NULL COMMENT '事件类型：STARTED FINISHED TOOL_CALLED ERROR APPROVAL',
    `tool_name`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '工具名（TOOL_CALLED/APPROVAL 时有值）',
    `tool_args`     TEXT         NULL COMMENT '工具入参（脱敏，JSON）',
    `tool_result`   TEXT         NULL COMMENT '工具结果（JSON，超长截断）',
    `duration`      INT          NOT NULL DEFAULT 0 COMMENT '耗时（毫秒）',
    `token_usage`   INT          NOT NULL DEFAULT 0 COMMENT 'Token 消耗',
    `step_count`    INT          NOT NULL DEFAULT 0 COMMENT '推理步数',
    `user_id`       BIGINT           NULL COMMENT '操作人ID',
    `user_type`     VARCHAR(16)  NOT NULL DEFAULT 'admin' COMMENT '操作人类型：admin-管理端 member-会员端',
    `tenant_id`     BIGINT       NOT NULL DEFAULT 0 COMMENT '租户ID（审计必带，用于隔离查询）',
    `result_code`   INT          NOT NULL DEFAULT 0 COMMENT '结果码：0-成功，其余为错误码',
    `result_msg`    VARCHAR(512) NOT NULL DEFAULT '' COMMENT '结果信息（失败时为异常信息）',
    `create_by`     BIGINT           NULL COMMENT '创建人ID',
    `create_time`   DATETIME         NULL COMMENT '创建时间',
    `update_by`     BIGINT           NULL COMMENT '更新人ID',
    `update_time`   DATETIME         NULL COMMENT '更新时间',
    `is_delete`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_event_type` (`event_type`),
    KEY `idx_tenant_time` (`tenant_id`, `create_time`),
    KEY `idx_agent_name` (`agent_name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 审计日志表';

-- ============================================================
-- 7. AI 用量汇总表：按租户/智能体/日聚合（成本上限与监控）
-- ============================================================
DROP TABLE IF EXISTS `agent_token_usage`;
CREATE TABLE `agent_token_usage` (
    `id`            BIGINT       NOT NULL COMMENT '主键ID（雪花算法）',
    `usage_date`    DATE         NOT NULL COMMENT '统计日期',
    `tenant_id`     BIGINT       NOT NULL DEFAULT 0 COMMENT '租户ID',
    `agent_name`    VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '智能体名',
    `request_count` INT          NOT NULL DEFAULT 0 COMMENT '请求次数',
    `input_tokens`  BIGINT       NOT NULL DEFAULT 0 COMMENT '输入 Token 累计',
    `output_tokens` BIGINT       NOT NULL DEFAULT 0 COMMENT '输出 Token 累计',
    `total_tokens`  BIGINT       NOT NULL DEFAULT 0 COMMENT '总 Token 累计',
    `cost`          DECIMAL(12, 4) NOT NULL DEFAULT 0 COMMENT '预估成本（按模型单价折算）',
    `create_by`     BIGINT           NULL COMMENT '创建人ID',
    `create_time`   DATETIME         NULL COMMENT '创建时间',
    `update_by`     BIGINT           NULL COMMENT '更新人ID',
    `update_time`   DATETIME         NULL COMMENT '更新时间',
    `is_delete`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_date_tenant_agent` (`usage_date`, `tenant_id`, `agent_name`, `is_delete`),
    KEY `idx_tenant_date` (`tenant_id`, `usage_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 用量汇总表';

-- ============================================================
-- 8. 初始化示例数据：公共 Agent 定义（source=2 管理端类型）
--    注：union 业务 Agent 由代码声明（source=1），不在此预置。
-- ============================================================
INSERT INTO `agent_definition`
    (`id`, `name`, `description`, `system_prompt`, `model`, `tool_names`, `streaming`, `max_steps`, `skills`, `group_code`, `source`, `version`, `status`)
VALUES
    (90001, 'ai-chat', '通用对话助手', '你是一位专业的智能助手，请友好、准确地回答用户问题。', 'qwen-max', NULL, 1, 6, NULL, 'common', 2, 1, 1),
    (90002, 'ai-translator', '多语言翻译', '你是一位多语言翻译专家，翻译需准确、地道、保留原文语气。', 'qwen-max', NULL, 1, 4, NULL, 'common', 2, 1, 1),
    (90003, 'ai-summary', '长文本摘要', '你是一位文本摘要专家，请提炼核心要点，输出简洁有条理的摘要。', 'qwen-max', NULL, 1, 4, NULL, 'common', 2, 1, 1);

-- ============================================================
-- 9. 菜单权限（ID 约定：顶级目录 3000/3010，子菜单 3100/3101/311x，按钮 菜单ID*10+序）
--    幂等：先删旧数据再插入；菜单授权需显式落库（已确认决策）。
-- ============================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` IN
    (3000, 3100, 3101, 31001, 3010, 3110, 31101, 31102, 31103, 3111, 3112, 31121, 31122, 3113);
DELETE FROM `sys_menu` WHERE `id` IN
    (3000, 3100, 3101, 31001, 3010, 3110, 31101, 31102, 31103, 3111, 3112, 31121, 31122, 3113);

-- 顶级目录：AI 助手
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `icon`, `sort`, `visible`, `status`)
VALUES (3000, 0, 'AI 助手', 1, '/ai', 'lucide:bot', 30, 1, 1);

-- AI 智能助手（对话工作台）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `component`, `sort`, `visible`, `status`)
VALUES (3100, 3000, 'AI 智能助手', 2, 'ai:assistant', '/ai/assistant', '/ai/assistant/index', 1, 1, 1);
-- 会话记录
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `component`, `sort`, `visible`, `status`)
VALUES (3101, 3000, '会话记录', 2, 'ai:session:list', '/ai/session', '/ai/session/index', 2, 1, 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (31001, 3101, '会话删除', 3, 'ai:session:delete', 1);

-- 顶级目录：智能体管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `icon`, `sort`, `visible`, `status`)
VALUES (3010, 0, '智能体管理', 1, '/agent', 'lucide:sparkles', 31, 1, 1);

-- Agent 管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `component`, `sort`, `visible`, `status`)
VALUES (3110, 3010, 'Agent 管理', 2, 'agent:definition:list', '/agent/definition', '/agent/definition/index', 1, 1, 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (31101, 3110, 'Agent新增', 3, 'agent:definition:create', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (31102, 3110, 'Agent修改', 3, 'agent:definition:update', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (31103, 3110, 'Agent删除', 3, 'agent:definition:delete', 3);

-- 工具管理（只读）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `component`, `sort`, `visible`, `status`)
VALUES (3111, 3010, '工具管理', 2, 'agent:tool:list', '/agent/tool', '/agent/tool/index', 2, 1, 1);

-- 审批中心
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `component`, `sort`, `visible`, `status`)
VALUES (3112, 3010, '审批中心', 2, 'agent:approval:list', '/agent/approval', '/agent/approval/index', 3, 1, 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (31121, 3112, '审批通过', 3, 'agent:approval:approve', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (31122, 3112, '审批驳回', 3, 'agent:approval:reject', 2);

-- AI 审计日志（只读）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `component`, `sort`, `visible`, `status`)
VALUES (3113, 3010, 'AI 审计日志', 2, 'agent:log:list', '/agent/log', '/agent/log/index', 4, 1, 1);

-- 授权给超级管理员（role_id=1，super_admin）：显式记录 AI 相关菜单权限
-- 说明：菜单授权需显式落库（已确认决策），不依赖 super_admin 通配；若角色编码不符请调整 role_id
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
VALUES
    (99001, 1, 3000), (99002, 1, 3100), (99003, 1, 3101), (99004, 1, 31001),
    (99005, 1, 3010), (99006, 1, 3110), (99007, 1, 31101), (99008, 1, 31102),
    (99009, 1, 31103), (99010, 1, 3111), (99011, 1, 3112), (99012, 1, 31121),
    (99013, 1, 31122), (99014, 1, 3113);
