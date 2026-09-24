-- ============================================================
-- 对话框多能力 P0：task_type 列 + 文案专用 Agent（增量 DDL + seed，MySQL 8.0+）
-- 适用：已部署环境升级；全新安装执行本文件（13/15/25 之后）
-- 关联方案：《对话框多能力补全与图像视频生成实现方案》P0
-- 背景：
--   task_type —— 前端意图/配额维度（chat/copywriting/docx/pdf/parse/image/video），
--                与 ai_model.capability（模型能力）两套语义；用于用量按类型统计、
--                配额按类型限次、历史消息产物类型徽标（用途闭合，非僵尸字段）。
--   ai-copywriter —— 文案方法论专用 Agent，供前端「文案生成」能力下拉显式选择；
--                仅注入 instruction、不自动切换会话归属（与 docx/pdf/parse 一致）。
-- 口径：task_type 默认 chat，存量行自动回填为 chat（旧数据均为纯对话，语义正确）；
--       Agent seed 幂等（先删后插）；会话列表查全部收紧为独立权限点 ai:session:query-all（31011），
--       未授予该点的管理端账号也只能查本人会话（方案 §4.4.2，与配额同批交付）。
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1. ai_chat_message 增加任务类型列（attachments 之后）
-- ------------------------------------------------------------
ALTER TABLE `ai_chat_message`
    ADD COLUMN `task_type` VARCHAR(16) NOT NULL DEFAULT 'chat'
        COMMENT '任务类型：chat/copywriting/docx/pdf/parse/image/video' AFTER `attachments`;

ALTER TABLE `ai_chat_message`
    ADD INDEX `idx_session_task` (`session_id`, `task_type`);

-- ------------------------------------------------------------
-- 2. 文案生成专用 Agent（source=2 管理端配置，工具为空纯提示词驱动）
-- ------------------------------------------------------------
DELETE FROM `agent_definition` WHERE `id` = 90004;
INSERT INTO `agent_definition`
    (`id`, `name`, `description`, `system_prompt`, `model`, `tool_names`, `streaming`, `max_steps`, `skills`, `group_code`, `source`, `version`, `status`)
VALUES
    (90004, 'ai-copywriter', '营销文案生成', '你是一位资深营销文案专家。请运用文案方法论（痛点-场景-卖点-证据-行动号召），
将用户提供的主题或素材重写为结构清晰、有感染力、可直接投放的营销文案。
要求：1) 先给出一句话核心卖点；2) 正文分「吸引注意/激发兴趣/建立信任/促成行动」四段；3) 语言凝练、口语化、避免空泛形容词；4) 结尾给明确行动号召。
仅输出文案本身，不要解释方法论。', 'deepseek-chat', '[]', 1, 3, NULL, 'common', 2, 1, 1);

-- ------------------------------------------------------------
-- 3. 会话查全部独立权限点（挂在会话记录菜单 3101 下，按钮型）
--    后端 AgentChatService.sessionPage 据此判定：无此点则强制注入本人 userId/userType。
-- ------------------------------------------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` = 31011;
DELETE FROM `sys_menu` WHERE `id` = 31011;
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`)
VALUES (31011, 3101, '会话查全部', 3, 'ai:session:query-all', 2);

-- 授权给超级管理员（role_id=1，super_admin）：显式记录，不依赖超管通配
DELETE FROM `sys_role_menu` WHERE `id` = 99029;
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (99029, 1, 31011);

-- ------------------------------------------------------------
-- 4. 校验
-- ------------------------------------------------------------
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT FROM INFORMATION_SCHEMA.COLUMNS
--   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_chat_message' AND COLUMN_NAME = 'task_type';
-- SELECT id, name, model, max_steps, status FROM agent_definition WHERE id = 90004;
-- SELECT id, permission FROM sys_menu WHERE id = 31011;
-- SELECT id, menu_id FROM sys_role_menu WHERE menu_id = 31011;
