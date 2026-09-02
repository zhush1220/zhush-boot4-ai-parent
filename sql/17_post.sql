-- ============================================================
-- 岗位管理：建表 + 用户表扩展 + 部门/岗位菜单与权限种子（MySQL 8.0+）
-- 依赖：01_admin_rbac.sql（sys_admin_user / sys_menu / sys_role_menu）
--      12_system_menu.sql / 09_union.sql（系统管理目录 id=100、sys_dept 表）
-- 幂等：菜单先删后插；用户表加列用 information_schema 判重
--
-- ID 规划（避开已占用的 101-106 / 110-163 / 1101 等）：
--   107  部门管理（菜单）
--   170~173  部门查询 / 新增 / 修改 / 删除
--   108  岗位管理（菜单）
--   180~183  岗位查询 / 新增 / 修改 / 删除
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 岗位表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post` (
    `id`          BIGINT       NOT NULL COMMENT '主键ID（雪花算法）',
    `post_code`   VARCHAR(64)  NOT NULL COMMENT '岗位编码',
    `post_name`   VARCHAR(64)  NOT NULL COMMENT '岗位名称',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `remark`      VARCHAR(255) NOT NULL DEFAULT '' COMMENT '备注',
    `create_by`   BIGINT           NULL COMMENT '创建人ID',
    `create_time` DATETIME         NULL COMMENT '创建时间',
    `update_by`   BIGINT           NULL COMMENT '更新人ID',
    `update_time` DATETIME         NULL COMMENT '更新时间',
    `is_delete`   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_post_code` (`post_code`),
    KEY `idx_post_name` (`post_name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '岗位表';

-- ------------------------------------------------------------
-- 用户表增加岗位ID（幂等：仅在列不存在时添加）
-- ------------------------------------------------------------
SET @exist := (SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_admin_user' AND COLUMN_NAME = 'post_id');
SET @sql := IF(@exist = 0,
    'ALTER TABLE `sys_admin_user` ADD COLUMN `post_id` BIGINT NULL COMMENT ''岗位ID'' AFTER `dept_id`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 部门 / 岗位 菜单与按钮权限点
-- ------------------------------------------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (107, 170, 171, 172, 173, 108, 180, 181, 182, 183);
DELETE FROM `sys_menu` WHERE `id` IN (107, 170, 171, 172, 173, 108, 180, 181, 182, 183);

-- 部门管理（菜单，component 对应前端 views/system/dept/index.vue）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
VALUES
    (107, 100, '部门管理', 2, '', 'dept', '/system/dept/index', 'lucide:network', 7, 1, 1);

-- 部门按钮权限点（type=3，permission 与后端 @SaCheckPermission 一致）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
VALUES
    (170, 107, '部门查询', 3, 'system:dept:query',  '', '', '', 1, 1, 1),
    (171, 107, '部门新增', 3, 'system:dept:create', '', '', '', 2, 1, 1),
    (172, 107, '部门修改', 3, 'system:dept:update', '', '', '', 3, 1, 1),
    (173, 107, '部门删除', 3, 'system:dept:delete', '', '', '', 4, 1, 1);

-- 岗位管理（菜单，component 对应前端 views/system/post/index.vue）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
VALUES
    (108, 100, '岗位管理', 2, '', 'post', '/system/post/index', 'lucide:user-cog', 8, 1, 1);

-- 岗位按钮权限点
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
VALUES
    (180, 108, '岗位查询', 3, 'system:post:query',  '', '', '', 1, 1, 1),
    (181, 108, '岗位新增', 3, 'system:post:create', '', '', '', 2, 1, 1),
    (182, 108, '岗位修改', 3, 'system:post:update', '', '', '', 3, 1, 1),
    (183, 108, '岗位删除', 3, 'system:post:delete', '', '', '', 4, 1, 1);

-- ------------------------------------------------------------
-- 超级管理员（role_id=1）关联菜单与权限点
-- ------------------------------------------------------------
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
VALUES
    (107, 1, 107),
    (170, 1, 170), (171, 1, 171), (172, 1, 172), (173, 1, 173),
    (108, 1, 108),
    (180, 1, 180), (181, 1, 181), (182, 1, 182), (183, 1, 183);
