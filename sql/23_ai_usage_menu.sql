-- ============================================================
-- AI 用量统计菜单与权限种子（MySQL 8.0+）
-- 关联方案：《Token 计量与审批续跑深化方案》分期 P2（用量日聚合）报表入口
-- 幂等：先删旧数据再插入（与 15_agent_model_sensitive.sql 风格一致）
-- 父目录：智能体管理 id=3010（见 13_agent_ai.sql）
-- 菜单 ID 约定：子菜单 311x（3114 模型管理、3115 敏感词库已占用），按钮 = 菜单ID*10+序
-- ============================================================

SET NAMES utf8mb4;

DELETE FROM `sys_role_menu` WHERE `menu_id` IN (3116);
DELETE FROM `sys_menu` WHERE `id` IN (3116);

-- 用量统计（只读报表，数据来源 agent_token_usage 日聚合表）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `component`, `sort`, `visible`, `status`)
VALUES (3116, 3010, '用量统计', 2, 'ai:usage:list', '/agent/usage', '/agent/usage/index', 7, 1, 1);

-- 授权给超级管理员（role_id=1，super_admin）：显式记录菜单权限
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
VALUES (99023, 1, 3116);

-- 校验：菜单与授权应各 1 条
-- SELECT `id`, `name`, `permission`, `path`, `component` FROM `sys_menu` WHERE `id` = 3116;
-- SELECT * FROM `sys_role_menu` WHERE `menu_id` = 3116;
