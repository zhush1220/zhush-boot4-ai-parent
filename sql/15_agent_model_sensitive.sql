-- ============================================================
-- AI 模型配置 + 敏感词库 动态化建表脚本
-- 关联方案：《AI 智能体模型配置与敏感词库动态化设计方案（v2）》
-- 字符集：utf8mb4 / utf8mb4_0900_ai_ci；主键统一雪花算法。
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 1. AI 模型配置表：多供应商模型动态管理（启停/默认/能力类型）
-- ============================================================
DROP TABLE IF EXISTS `ai_model`;
CREATE TABLE `ai_model` (
    `id`            BIGINT        NOT NULL COMMENT '主键ID（雪花算法）',
    `code`          VARCHAR(64)   NOT NULL COMMENT '模型编码（唯一，Agent 定义 model 字段引用，如 qwen-max）',
    `name`          VARCHAR(128)  NOT NULL COMMENT '展示名（如 通义千问-Max）',
    `type`          VARCHAR(32)   NOT NULL COMMENT '供应商标识：dashscope / openai / ollama',
    `api_key`       VARCHAR(1024) NOT NULL DEFAULT '' COMMENT 'API Key（AES 加密密文，支持 ${ENV} 占位）',
    `base_url`      VARCHAR(256)  NOT NULL DEFAULT '' COMMENT '服务地址（openai 兼容服务）',
    `model`         VARCHAR(128)  NOT NULL COMMENT '实际模型名（如 qwen-max、wanx2.1-t2i）',
    `capability`    VARCHAR(16)   NOT NULL DEFAULT 'text' COMMENT '能力类型：text-文本 image-图片 video-视频 audio-语音',
    `input_price`   DECIMAL(12, 6) NOT NULL DEFAULT 0 COMMENT '输入单价（元/千 token，0=未定价）',
    `output_price`  DECIMAL(12, 6) NOT NULL DEFAULT 0 COMMENT '输出单价（元/千 token，0=未定价）',
    `is_default`    TINYINT       NOT NULL DEFAULT 0 COMMENT '是否默认模型：0-否 1-是（全局唯一）',
    `priority`      INT           NOT NULL DEFAULT 0 COMMENT '优先级（下拉排序，越大越靠前）',
    `options`       JSON          NULL COMMENT '透传参数（JSON：temperature/top_p/size 等）',
    `status`        TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：0-停用 1-启用',
    `remark`        VARCHAR(512)  NOT NULL DEFAULT '' COMMENT '备注',
    `create_by`     BIGINT            NULL COMMENT '创建人ID',
    `create_time`   DATETIME          NULL COMMENT '创建时间',
    `update_by`     BIGINT            NULL COMMENT '更新人ID',
    `update_time`   DATETIME          NULL COMMENT '更新时间',
    `is_delete`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`, `is_delete`),
    KEY `idx_status_capability` (`status`, `capability`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 模型配置表';

-- ============================================================
-- 2. AI 敏感词库表：审批门禁分级命中（提示/审批/拦截）
-- ============================================================
DROP TABLE IF EXISTS `ai_sensitive_word`;
CREATE TABLE `ai_sensitive_word` (
    `id`           BIGINT       NOT NULL COMMENT '主键ID（雪花算法）',
    `word`         VARCHAR(64)  NOT NULL COMMENT '敏感词',
    `match_type`   VARCHAR(16)  NOT NULL DEFAULT 'tool_name' COMMENT '匹配对象：tool_name-工具名 content-入参/内容 both-两者',
    `level`        TINYINT      NOT NULL DEFAULT 2 COMMENT '命中级别：1-提示 2-审批 3-拦截',
    `group_code`   VARCHAR(32)  NOT NULL DEFAULT 'common' COMMENT '业务分组（默认 common）',
    `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-停用 1-启用',
    `remark`       VARCHAR(512) NOT NULL DEFAULT '' COMMENT '备注',
    `create_by`    BIGINT           NULL COMMENT '创建人ID',
    `create_time`  DATETIME         NULL COMMENT '创建时间',
    `update_by`    BIGINT           NULL COMMENT '更新人ID',
    `update_time`  DATETIME         NULL COMMENT '更新时间',
    `is_delete`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_word` (`word`, `is_delete`),
    KEY `idx_status_level` (`status`, `level`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 敏感词库表';

-- ============================================================
-- 3. 初始化数据：对齐 application-dev.yaml 存量模型配置（迁移即可用）
-- ============================================================
INSERT INTO `ai_model`(`id`, `code`, `name`, `type`, `api_key`, `base_url`, `model`, `capability`, `is_default`, `priority`, `status`) VALUES
  (93001, 'qwen-max',      '通义千问-Max',  'dashscope', '${DASHSCOPE_API_KEY:}', '',                              'qwen-max',      'text', 0, 10, 1),
  (93002, 'deepseek-chat', 'DeepSeek-Chat', 'openai',    '${DEEPSEEK_API_KEY:}',  'https://api.deepseek.com/v1', 'deepseek-chat', 'text', 1, 9,  1),
  (93003, 'local-qwen',    '本地 Ollama',   'ollama',    '',                      'http://localhost:11434',      'qwen2.5:7b',    'text', 0, 1,  0);

INSERT INTO `ai_sensitive_word`(`id`, `word`, `match_type`, `level`, `status`) VALUES
  (94001, '删除', 'tool_name', 2, 1),
  (94002, '转账', 'both',      3, 1),
  (94003, '下单', 'tool_name', 2, 1),
  (94004, '退款', 'tool_name', 2, 1);

-- ============================================================
-- 4. 菜单权限（挂智能体管理目录 3010 下；按钮 = 菜单ID*10+序）
--    幂等：先删旧数据再插入；菜单授权显式落库（已确认决策）。
-- ============================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` IN
    (3114, 31141, 31142, 31143, 3115, 31151, 31152, 31153);
DELETE FROM `sys_menu` WHERE `id` IN
    (3114, 31141, 31142, 31143, 3115, 31151, 31152, 31153);

-- 模型管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `component`, `sort`, `visible`, `status`)
VALUES (3114, 3010, '模型管理', 2, 'ai:model:list', '/agent/model', '/agent/model/index', 5, 1, 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (31141, 3114, '模型新增', 3, 'ai:model:create', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (31142, 3114, '模型修改', 3, 'ai:model:update', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (31143, 3114, '模型删除', 3, 'ai:model:delete', 3);

-- 敏感词库
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `component`, `sort`, `visible`, `status`)
VALUES (3115, 3010, '敏感词库', 2, 'ai:sensitive:list', '/agent/sensitive', '/agent/sensitive/index', 6, 1, 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (31151, 3115, '敏感词新增', 3, 'ai:sensitive:create', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (31152, 3115, '敏感词修改', 3, 'ai:sensitive:update', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (31153, 3115, '敏感词删除', 3, 'ai:sensitive:delete', 3);

-- 授权给超级管理员（role_id=1，super_admin）：显式记录菜单权限
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
VALUES
    (99015, 1, 3114), (99016, 1, 31141), (99017, 1, 31142), (99018, 1, 31143),
    (99019, 1, 3115), (99020, 1, 31151), (99021, 1, 31152), (99022, 1, 31153);
