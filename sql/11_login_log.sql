-- ============================================================
-- 登录日志表 DDL（MySQL 8.0+）
-- 字符集：utf8mb4    存储引擎：InnoDB
-- 主键：雪花算法（应用层生成，BIGINT）
-- 公共字段：create_by / create_time / update_by / update_time / is_delete
--          与 com.zhush.common.entity.BaseEntity 对应
-- 写入方：com.zhush.system.service.LoginLogService（登录成功/失败均异步记录）
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 登录日志表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log` (
    `id`           BIGINT       NOT NULL COMMENT '主键ID（雪花算法）',
    `user_id`      BIGINT           NULL COMMENT '登录人ID（账号不存在等场景为 NULL）',
    `username`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '登录账号（登录时填写的账号）',
    `user_type`    VARCHAR(16)  NOT NULL DEFAULT 'admin' COMMENT '登录人类型：admin-管理端 member-会员端',
    `login_ip`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '登录IP',
    `login_region` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '登录IP归属地（如 中国 广东省 深圳市）',
    `user_agent`   VARCHAR(512) NOT NULL DEFAULT '' COMMENT '浏览器UA',
    `device`       VARCHAR(16)  NOT NULL DEFAULT 'PC' COMMENT '登录设备：PC/H5/APP 等',
    `login_type`   VARCHAR(16)  NOT NULL DEFAULT 'password' COMMENT '登录方式：password-账号密码 social-第三方',
    `result_code`  INT          NOT NULL DEFAULT 0 COMMENT '结果码：0-成功，其余为错误码',
    `result_msg`   VARCHAR(256) NOT NULL DEFAULT '' COMMENT '结果提示信息（失败时为失败原因）',
    `create_by`    BIGINT           NULL COMMENT '创建人ID',
    `create_time`  DATETIME         NULL COMMENT '创建时间（即登录时间）',
    `update_by`    BIGINT           NULL COMMENT '更新人ID',
    `update_time`  DATETIME         NULL COMMENT '更新时间',
    `is_delete`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_username` (`username`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '登录日志表';

-- ============================================================
-- 菜单权限（ID 约定：目录 1000 / 菜单 11xx / 按钮 菜单ID*10+序）
-- 幂等：先删旧数据再插入；超级管理员（super_admin）自带 * 通配权限，无需授权
-- ============================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (1102, 11021);
DELETE FROM `sys_menu` WHERE `id` IN (1102, 11021);

-- 登录日志（挂在系统管理目录下）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (1102, 1000, '登录日志', 2, '/system/login-log', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (11021, 1102, '登录日志查询', 3, 'system:login-log:query', 1);
