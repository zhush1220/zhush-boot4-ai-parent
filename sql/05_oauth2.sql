-- ============================================================
-- OAuth2 开放平台 DDL（MySQL 8.0+）
-- 字符集：utf8mb4    存储引擎：InnoDB
-- 主键：雪花算法（应用层生成，BIGINT）
-- 公共字段：create_by / create_time / update_by / update_time / is_delete
--          与 com.zhush.common.entity.BaseEntity 对应
-- 说明：令牌/授权码由 sa-token-oauth2 存 Redis，本文件仅客户端与调用日志两张表
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- OAuth2 客户端表（第三方应用注册信息）
-- 写入方：com.zhush.system.service.Oauth2ClientService
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_oauth2_client`;
CREATE TABLE `sys_oauth2_client` (
    `id`                    BIGINT        NOT NULL COMMENT '主键ID（雪花算法）',
    `client_id`             VARCHAR(64)   NOT NULL COMMENT '客户端标识',
    `client_secret`         VARCHAR(128)  NOT NULL COMMENT '客户端密钥（BCrypt 存储，仅创建时明文回显一次）',
    `client_name`           VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '应用名称（授权确认页展示）',
    `logo`                  VARCHAR(256)  NOT NULL DEFAULT '' COMMENT '应用图标URL（授权确认页展示）',
    `grant_types`           VARCHAR(256)  NOT NULL DEFAULT '' COMMENT '允许的授权类型，逗号分隔（authorization_code,refresh_token,client_credentials）',
    `scopes`                VARCHAR(512)  NOT NULL DEFAULT '' COMMENT '允许申请的 scope，逗号分隔（openid,userinfo,roles,...）',
    `redirect_uris`         VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '回调地址白名单，逗号分隔，精确匹配',
    `access_token_timeout`  BIGINT            NULL COMMENT 'access_token 有效期（秒），NULL 用全局默认',
    `refresh_token_timeout` BIGINT            NULL COMMENT 'refresh_token 有效期（秒），NULL 用全局默认',
    `qps_limit`             INT               NULL COMMENT '开放 API 限流 QPS，NULL 用全局默认',
    `daily_quota`           INT               NULL COMMENT '开放 API 日调用配额，NULL 用全局默认',
    `notify_url`            VARCHAR(256)  NOT NULL DEFAULT '' COMMENT '回调通知地址（预留，事件推送用）',
    `auto_confirm`          TINYINT       NOT NULL DEFAULT 0 COMMENT '是否免授权确认页：0-否 1-是（自有信任应用）',
    `status`                TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：0-停用 1-启用（停用后存量令牌立即失效）',
    `remark`                VARCHAR(256)  NOT NULL DEFAULT '' COMMENT '备注',
    `create_by`             BIGINT            NULL COMMENT '创建人ID',
    `create_time`           DATETIME          NULL COMMENT '创建时间',
    `update_by`             BIGINT            NULL COMMENT '更新人ID',
    `update_time`           DATETIME          NULL COMMENT '更新时间',
    `is_delete`             TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_client_id` (`client_id`, `is_delete`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'OAuth2 客户端表';

-- ------------------------------------------------------------
-- 开放 API 调用日志表
-- 写入方：com.zhush.system.service.OpenApiLogService（OpenApiInterceptor 采集，异步落库）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_open_api_log`;
CREATE TABLE `sys_open_api_log` (
    `id`             BIGINT       NOT NULL COMMENT '主键ID（雪花算法）',
    `client_id`      VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '客户端标识（鉴权失败无法解析时为空）',
    `login_id`       BIGINT           NULL COMMENT '授权用户ID（应用级调用为 NULL）',
    `request_method` VARCHAR(16)  NOT NULL DEFAULT '' COMMENT '请求方式（GET/POST/PUT/DELETE）',
    `request_uri`    VARCHAR(256) NOT NULL DEFAULT '' COMMENT '请求URI',
    `status`         INT          NOT NULL DEFAULT 0 COMMENT 'HTTP 响应状态码',
    `error_code`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT 'OAuth2 协议错误码（invalid_token/insufficient_scope...，成功为空）',
    `cost_millis`    BIGINT       NOT NULL DEFAULT 0 COMMENT '执行耗时（毫秒）',
    `client_ip`      VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '调用方IP',
    `call_time`      DATETIME         NULL COMMENT '调用时间',
    `create_by`      BIGINT           NULL COMMENT '创建人ID',
    `create_time`    DATETIME         NULL COMMENT '创建时间',
    `update_by`      BIGINT           NULL COMMENT '更新人ID',
    `update_time`    DATETIME         NULL COMMENT '更新时间',
    `is_delete`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_client_id` (`client_id`),
    KEY `idx_call_time` (`call_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '开放 API 调用日志表';

-- ------------------------------------------------------------
-- 菜单（OAuth2 客户端管理 / 开放 API 日志查询权限，挂在系统管理目录下按需调整 parent_id）
-- ------------------------------------------------------------
-- INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `sort`)
-- VALUES (企业自行分配ID, 系统管理目录ID, 'OAuth2客户端', 2, 'system:oauth2-client:query', '/system/oauth2-client', 100);
-- INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `sort`)
-- VALUES (企业自行分配ID, 系统管理目录ID, '开放API日志', 2, 'system:open-api-log:query', '/system/open-api-log', 101);
