-- ============================================================
-- 系统监控-数据/缓存/服务监控权限配置（MySQL 8.0+）
-- 前置依赖：13_system_job.sql（已创建一级目录「系统监控」id=2000）
-- 字符集：utf8mb4    存储引擎：InnoDB
--
-- 菜单 ID 规划（系统监控段，2000 起）：
--   2000    系统监控（目录，已在 13_system_job.sql 创建）
--   2001    定时任务（已在 13 创建）  20011~20016/20021~20022 按钮
--   2005    数据监控（Druid）         20051~20052 按钮
--   2006    缓存监控（Redis）         20061~20062 按钮
--   2007    服务监控                   20071 按钮
-- ⚠️ 不可占用 menuId=2002（其按钮段 2002x 与调度日志按钮 20021/20022 冲突）
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 菜单权限（目录 2000 已存在；新增数据/缓存/服务监控及其按钮）
-- 幂等：先删旧数据再插入；超级管理员（role_id=1）关联全部新菜单与按钮
-- ------------------------------------------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (2005, 2006, 2007, 20051, 20052, 20061, 20062, 20071);
DELETE FROM `sys_menu` WHERE `id` IN (2005, 2006, 2007, 20051, 20052, 20061, 20062, 20071);

-- 页面菜单（component 对应前端 views 目录：monitor/druid|cache|server/index.vue）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
VALUES
    (2005, 2000, '数据监控', 2, 'druid',  '/monitor/druid/index',  'lucide:database', 2, 1, 1),
    (2006, 2000, '缓存监控', 2, 'cache',  '/monitor/cache/index',  'lucide:box',      3, 1, 1),
    (2007, 2000, '服务监控', 2, 'server', '/monitor/server/index', 'lucide:server',   4, 1, 1);

-- 按钮权限点（type=3，permission 与后端 @SaCheckPermission 一致）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
VALUES
    -- 数据监控
    (20051, 2005, '数据监控查询', 3, 'system:monitor:druid:query',  '', '', '', 1, 1, 1),
    (20052, 2005, '数据监控重置', 3, 'system:monitor:druid:reset',  '', '', '', 2, 1, 1),
    -- 缓存监控
    (20061, 2006, '缓存监控查询', 3, 'system:monitor:cache:query',  '', '', '', 1, 1, 1),
    (20062, 2006, '缓存监控删除', 3, 'system:monitor:cache:delete', '', '', '', 2, 1, 1),
    -- 服务监控
    (20071, 2007, '服务监控查询', 3, 'system:monitor:server:query', '', '', '', 1, 1, 1);

-- 超级管理员（role_id=1）关联全部新菜单与权限点
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
VALUES
    (20050, 1, 2005),
    (20051, 1, 20051), (20052, 1, 20052),
    (20060, 1, 2006),
    (20061, 1, 20061), (20062, 1, 20062),
    (20070, 1, 2007),
    (20071, 1, 20071);
