-- ============================================================
-- 数据权限落地：管理员敏感字段的列级权限点（MySQL 8.0+）
-- 关联代码：AdminUserVO 的 @FieldPermission（mobile / email / loginIp）
--          AdminUserService#getUserPage 的 @DataPermission（行级，复用 dept_id 列）
-- 幂等：先删旧数据再插入（与 23_ai_usage_menu.sql 风格一致）
-- 父菜单：用户管理 id=101（见 12_system_menu.sql），按钮权限点延续 11x 序号
-- ============================================================

SET NAMES utf8mb4;

DELETE FROM `sys_role_menu` WHERE `menu_id` IN (116, 117, 118);
DELETE FROM `sys_menu` WHERE `id` IN (116, 117, 118);

-- 列级权限点：拥有对应权限才能看到该列明文，否则接口返回空值
-- 说明：type=3（按钮）不会进入导航菜单树（AdminMenuService 按 type<>3 过滤），与 110~115 保持一致取 visible=1
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
VALUES (116, 101, '手机号明文', 3, 'system:user:mobile', '', '', '', 7, 1, 1),
       (117, 101, '邮箱明文', 3, 'system:user:email', '', '', '', 8, 1, 1),
       (118, 101, '登录IP明文', 3, 'system:user:login-ip', '', '', '', 9, 1, 1);

-- 授权给超级管理员（role_id=1，super_admin）：其权限标识为 * 通配，显式记录便于审计
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
VALUES (116, 1, 116),
       (117, 1, 117),
       (118, 1, 118);

-- ------------------------------------------------------------
-- 说明：
-- 1. 权限点未授予的角色，总开关开启后这些列会立即返回空值（列表页同步隐藏该列），
--    因此上线时需按业务需要为已有角色勾选，例如给运营角色补授权：
--    INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES (201, 2, 116);
-- 2. sys_role.data_scope 默认 1（全部数据），存量角色行为不变；需要按部门/本人隔离时
--    通过「角色管理 - 数据范围」界面设置，或手工执行：
--    UPDATE sys_role SET data_scope = 2 WHERE id = ?;   -- 本部门
--    UPDATE sys_role SET data_scope = 5, data_scope_dept_ids = '101,102' WHERE id = ?;  -- 自定义
-- 3. 行级/列级过滤均需应用开启总开关：zhush.data-permission.enabled=true
-- 4. 后续业务表增加受控列时，按「模块:业务实体:字段」新增权限点并同步 VO 注解，
--    接入步骤见 docs/数据权限使用指南.md 的「列级权限跨模块接入规范」
-- ------------------------------------------------------------
