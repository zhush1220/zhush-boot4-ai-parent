-- ============================================================
-- 操作日志 / 登录日志 菜单与权限种子（MySQL 8.0+）
-- 幂等：先删旧数据再插入（与 11_login_log.sql 风格一致）
-- 父目录：系统管理 id=100（见 12_system_menu.sql）
-- 超管（super_admin）自带 * 通配权限，无需授权；此处仍补角色关联便于演示
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 菜单与按钮权限点
-- ------------------------------------------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (1101, 11011, 1102, 11021);
DELETE FROM `sys_menu` WHERE `id` IN (1101, 11011, 1102, 11021);

-- 操作日志（菜单，component 对应前端 views/system/operate-log/index.vue）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
VALUES
    (1101, 100, '操作日志', 2, '', 'operate-log', '/system/operate-log/index', 'lucide:file-text', 4, 1, 1);

INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
VALUES
    (11011, 1101, '操作日志查询', 3, 'system:operate-log:query', '', '', '', 1, 1, 1);

-- 登录日志（菜单，component 对应前端 views/system/login-log/index.vue）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
VALUES
    (1102, 100, '登录日志', 2, '', 'login-log', '/system/login-log/index', 'lucide:history', 5, 1, 1);

INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
VALUES
    (11021, 1102, '登录日志查询', 3, 'system:login-log:query', '', '', '', 1, 1, 1);

-- 超级管理员（role_id=1）关联日志菜单与权限点
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
VALUES
    (1101, 1, 1101),
    (11011, 1, 11011),
    (1102, 1, 1102),
    (11021, 1, 11021);
