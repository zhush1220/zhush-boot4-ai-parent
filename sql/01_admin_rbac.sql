-- ============================================================
-- 管理端 RBAC 权限模型 DDL（MySQL 8.0+）
-- 字符集：utf8mb4    存储引擎：InnoDB
-- 主键：雪花算法（应用层生成，BIGINT）
-- 公共字段：create_by / create_time / update_by / update_time / is_delete
--          与 com.zhush.common.entity.BaseEntity 对应
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 管理员用户表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_admin_user`;
CREATE TABLE `sys_admin_user` (
    `id`          BIGINT       NOT NULL COMMENT '主键ID（雪花算法）',
    `username`    VARCHAR(64)  NOT NULL COMMENT '登录账号',
    `password`    VARCHAR(100) NOT NULL DEFAULT '' COMMENT '登录密码（BCrypt 加密）',
    `nickname`    VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '昵称',
    `real_name`   VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '真实姓名',
    `avatar`      VARCHAR(512) NOT NULL DEFAULT '' COMMENT '头像地址',
    `mobile`      VARCHAR(20)  NOT NULL DEFAULT '' COMMENT '手机号',
    `email`       VARCHAR(128) NOT NULL DEFAULT '' COMMENT '邮箱',
    `dept_id`     BIGINT           NULL COMMENT '部门ID（预留）',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `login_ip`    VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '最后登录IP',
    `login_date`  DATETIME         NULL COMMENT '最后登录时间',
    `remark`      VARCHAR(512) NOT NULL DEFAULT '' COMMENT '备注',
    `create_by`   BIGINT           NULL COMMENT '创建人ID',
    `create_time` DATETIME         NULL COMMENT '创建时间',
    `update_by`   BIGINT           NULL COMMENT '更新人ID',
    `update_time` DATETIME         NULL COMMENT '更新时间',
    `is_delete`   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`, `is_delete`),
    KEY `idx_mobile` (`mobile`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '管理员用户表';

-- ------------------------------------------------------------
-- 角色表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
    `id`          BIGINT       NOT NULL COMMENT '主键ID（雪花算法）',
    `name`        VARCHAR(64)  NOT NULL COMMENT '角色名称',
    `code`        VARCHAR(64)  NOT NULL COMMENT '角色编码（如 admin、ops）',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `data_scope`  TINYINT      NOT NULL DEFAULT 1 COMMENT '数据权限：1-全部 2-本部门 3-本部门及以下 4-仅本人 5-自定义（预留）',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `remark`      VARCHAR(512) NOT NULL DEFAULT '' COMMENT '备注',
    `create_by`   BIGINT           NULL COMMENT '创建人ID',
    `create_time` DATETIME         NULL COMMENT '创建时间',
    `update_by`   BIGINT           NULL COMMENT '更新人ID',
    `update_time` DATETIME         NULL COMMENT '更新时间',
    `is_delete`   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`, `is_delete`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色表';

-- ------------------------------------------------------------
-- 菜单 / 权限表（目录、菜单、按钮三级）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
    `id`          BIGINT       NOT NULL COMMENT '主键ID（雪花算法）',
    `parent_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '父菜单ID（根为0）',
    `name`        VARCHAR(64)  NOT NULL COMMENT '菜单名称',
    `type`        TINYINT      NOT NULL COMMENT '类型：1-目录 2-菜单 3-按钮',
    `permission`  VARCHAR(128) NOT NULL DEFAULT '' COMMENT '权限标识（如 system:user:add）',
    `path`        VARCHAR(256) NOT NULL DEFAULT '' COMMENT '路由地址',
    `component`   VARCHAR(256) NOT NULL DEFAULT '' COMMENT '前端组件路径',
    `icon`        VARCHAR(128) NOT NULL DEFAULT '' COMMENT '菜单图标',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `visible`     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否可见：0-隐藏 1-显示',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `create_by`   BIGINT           NULL COMMENT '创建人ID',
    `create_time` DATETIME         NULL COMMENT '创建时间',
    `update_by`   BIGINT           NULL COMMENT '更新人ID',
    `update_time` DATETIME         NULL COMMENT '更新时间',
    `is_delete`   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '菜单权限表';

-- ------------------------------------------------------------
-- 管理员-角色关联表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_admin_user_role`;
CREATE TABLE `sys_admin_user_role` (
    `id`          BIGINT   NOT NULL COMMENT '主键ID（雪花算法）',
    `user_id`     BIGINT   NOT NULL COMMENT '管理员用户ID',
    `role_id`     BIGINT   NOT NULL COMMENT '角色ID',
    `create_by`   BIGINT       NULL COMMENT '创建人ID',
    `create_time` DATETIME     NULL COMMENT '创建时间',
    `update_by`   BIGINT       NULL COMMENT '更新人ID',
    `update_time` DATETIME     NULL COMMENT '更新时间',
    `is_delete`   TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '管理员角色关联表';

-- ------------------------------------------------------------
-- 角色-菜单关联表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
    `id`          BIGINT   NOT NULL COMMENT '主键ID（雪花算法）',
    `role_id`     BIGINT   NOT NULL COMMENT '角色ID',
    `menu_id`     BIGINT   NOT NULL COMMENT '菜单ID',
    `create_by`   BIGINT       NULL COMMENT '创建人ID',
    `create_time` DATETIME     NULL COMMENT '创建时间',
    `update_by`   BIGINT       NULL COMMENT '更新人ID',
    `update_time` DATETIME     NULL COMMENT '更新时间',
    `is_delete`   TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_menu_id` (`menu_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色菜单关联表';

-- ------------------------------------------------------------
-- 管理端第三方登录绑定表（预留：企业微信、钉钉等）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_social_user`;
CREATE TABLE `sys_social_user` (
    `id`          BIGINT       NOT NULL COMMENT '主键ID（雪花算法）',
    `user_id`     BIGINT       NOT NULL COMMENT '管理员用户ID',
    `social_type` VARCHAR(32)  NOT NULL COMMENT '第三方类型：WECOM-企业微信 DINGTALK-钉钉 ...',
    `open_id`     VARCHAR(128) NOT NULL COMMENT '第三方用户唯一标识',
    `union_id`    VARCHAR(128) NOT NULL DEFAULT '' COMMENT '第三方 UnionId',
    `nickname`    VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '第三方昵称',
    `avatar`      VARCHAR(512) NOT NULL DEFAULT '' COMMENT '第三方头像',
    `raw_info`    VARCHAR(2048) NOT NULL DEFAULT '' COMMENT '第三方原始信息（JSON）',
    `create_by`   BIGINT           NULL COMMENT '创建人ID',
    `create_time` DATETIME         NULL COMMENT '创建时间',
    `update_by`   BIGINT           NULL COMMENT '更新人ID',
    `update_time` DATETIME         NULL COMMENT '更新时间',
    `is_delete`   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_openid` (`social_type`, `open_id`, `is_delete`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '管理端第三方登录绑定表';

-- ============================================================
-- 初始化数据（口令均为 BCrypt("123456")，登录后请立即修改）
-- ============================================================
INSERT INTO `sys_admin_user` (`id`, `username`, `password`, `nickname`, `real_name`, `status`, `remark`)
VALUES (1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '超级管理员', '超级管理员', 1, '内置超级管理员');

INSERT INTO `sys_role` (`id`, `name`, `code`, `sort`, `status`, `remark`)
VALUES (1, '超级管理员', 'super_admin', 1, 1, '拥有全部权限');

INSERT INTO `sys_admin_user_role` (`id`, `user_id`, `role_id`) VALUES (1, 1, 1);
