-- ============================================================
-- 对话框自然语言多能力（增量 DDL + 配置，MySQL 8.0+）
-- 适用：已部署环境升级；全新安装执行 13_agent_ai.sql 即可（已含本文件变更）
-- 关联方案：《对话框自然语言多能力任务实现方案》P0-P2
-- 背景：生成能力（generateDocument：Markdown→Word/PDF）与解析能力（parseDocument：
--       文档→纯文本）以 @AgentTool 工具挂载到 ai-chat 智能体，由对话大模型按意图自主调用：
--       attachments —— assistant 消息产物附件 JSON（下载卡片回显载体）；
--       tool_names  —— DefaultToolkitRegistry 最小暴露原则下为空即零工具，须显式配置；
--       system_prompt —— 追加能力与工具调用约定，引导模型正确选工具与回链，max_steps
--       由 6 调大到 8（生成正文 + 工具调用 + 回链的多步 ReAct 余量）。
-- 口径：UPDATE 仅针对 ai-chat（90001），不动其他智能体；执行一次即可，重复执行幂等。
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1. ai_chat_message 增加产物附件列
-- ------------------------------------------------------------
ALTER TABLE `ai_chat_message`
    ADD COLUMN `attachments` JSON NULL COMMENT '产物附件（assistant，JSON 数组：type/format/name/url/size）' AFTER `tool_calls`;

-- ------------------------------------------------------------
-- 2. ai-chat 装配生成/解析工具 + 能力说明 prompt + ReAct 步数余量
--    （与 13_agent_ai.sql seed 保持一致）
-- ------------------------------------------------------------
UPDATE `agent_definition`
SET `tool_names`    = '["generateDocument", "parseDocument"]',
    `system_prompt` = '你是一位专业的智能助手，请友好、准确地回答用户问题。\n\n【能力与工具约定】\n1. 生成文档：当用户要求导出/生成 Word、PDF 或下载文档时，先产出结构完整的 Markdown 正文（标题/段落/列表/表格），再调用 generateDocument 工具（content=正文，format=docx|pdf，缺省 docx），将返回结果中 artifact 的链接以 Markdown 文件链接形式附在回复末尾，勿自行编造下载地址。\n2. 解析附件：当用户消息含【附件】name: url 时，如需了解文件内容调用 parseDocument（fileUrl=url）；未上传附件时不要调用。\n3. 选工具还是直答由你按意图判断；与工具无关的请求不要调用工具。',
    `max_steps`     = 8,
    `version`       = `version` + 1
WHERE `id` = 90001 AND `name` = 'ai-chat';

-- ------------------------------------------------------------
-- 3. 校验：attachments 列应存在；ai-chat 工具与 prompt 应已配置
-- ------------------------------------------------------------
-- SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
--   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_chat_message' AND COLUMN_NAME = 'attachments';
-- SELECT tool_names, max_steps, version FROM agent_definition WHERE id = 90001;
