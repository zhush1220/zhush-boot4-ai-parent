-- ============================================================
-- 移除业务智能体与 ai-copywriter（仅保留 ai-chat/ai-translator/ai-summary）
-- 背景：产品决策精简智能体列表，删除所有 union-* 业务 Agent 及 ai-copywriter。
--       代码层 UnionAgentDefinitionProvider/UnionAgentTools 已删除，
--       DB 中可能通过管理端或历史脚本插入了这些记录，需显式清理。
-- ============================================================

SET NAMES utf8mb4;

-- 删除业务智能体（union-selector、union-copywriter、union-compliance 等）
DELETE FROM `agent_definition` WHERE `name` LIKE 'union-%';

-- 删除文案专用 Agent（source=2 管理端配置型，id=90004）
DELETE FROM `agent_definition` WHERE `id` = 90004 AND `name` = 'ai-copywriter';
