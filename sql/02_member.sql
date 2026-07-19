-- ============================================================
-- 会员端用户模型 DDL（MySQL 8.0+）
-- 字符集：utf8mb4    存储引擎：InnoDB
-- 说明：会员端无权限模型（无 RBAC），仅登录态；支持微信小程序 + 账号密码登录，
--       并预留其他第三方授权（微信开放平台、支付宝、Apple 等）。
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 会员用户表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `member_user`;
CREATE TABLE `member_user` (
    `id`           BIGINT       NOT NULL COMMENT '主键ID（雪花算法）',
    `mobile`       VARCHAR(20)  NOT NULL DEFAULT '' COMMENT '手机号',
    `password`     VARCHAR(100) NOT NULL DEFAULT '' COMMENT '登录密码（BCrypt，未设置为空）',
    `nickname`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '昵称',
    `avatar`       VARCHAR(512) NOT NULL DEFAULT '' COMMENT '头像地址',
    `gender`       TINYINT      NOT NULL DEFAULT 0 COMMENT '性别：0-未知 1-男 2-女',
    `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `register_ip`  VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '注册IP',
    `register_type` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '注册来源：WECHAT_MINI-小程序 PASSWORD-账号密码',
    `login_ip`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '最后登录IP',
    `login_date`   DATETIME         NULL COMMENT '最后登录时间',
    `create_by`    BIGINT           NULL COMMENT '创建人ID',
    `create_time`  DATETIME         NULL COMMENT '创建时间',
    `update_by`    BIGINT           NULL COMMENT '更新人ID',
    `update_time`  DATETIME         NULL COMMENT '更新时间',
    `is_delete`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_mobile` (`mobile`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '会员用户表';

-- ------------------------------------------------------------
-- 会员第三方授权表（微信小程序、微信开放平台、支付宝等）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `member_social_user`;
CREATE TABLE `member_social_user` (
    `id`           BIGINT       NOT NULL COMMENT '主键ID（雪花算法）',
    `user_id`      BIGINT       NOT NULL COMMENT '会员用户ID',
    `social_type`  VARCHAR(32)  NOT NULL COMMENT '第三方类型：WECHAT_MINI-微信小程序 WECHAT_MP-微信公众号 WECHAT_OPEN-开放平台 ALIPAY APPLE ...',
    `open_id`      VARCHAR(128) NOT NULL COMMENT '第三方用户唯一标识（openid）',
    `union_id`     VARCHAR(128) NOT NULL DEFAULT '' COMMENT '第三方 UnionId（同主体多应用打通）',
    `session_key`  VARCHAR(128) NOT NULL DEFAULT '' COMMENT '微信 session_key（小程序）',
    `nickname`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '第三方昵称',
    `avatar`       VARCHAR(512) NOT NULL DEFAULT '' COMMENT '第三方头像',
    `raw_info`     VARCHAR(2048) NOT NULL DEFAULT '' COMMENT '第三方原始信息（JSON）',
    `create_by`    BIGINT           NULL COMMENT '创建人ID',
    `create_time`  DATETIME         NULL COMMENT '创建时间',
    `update_by`    BIGINT           NULL COMMENT '更新人ID',
    `update_time`  DATETIME         NULL COMMENT '更新时间',
    `is_delete`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_openid` (`social_type`, `open_id`, `is_delete`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_union_id` (`union_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '会员第三方授权表';
