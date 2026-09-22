-- ============================================================
-- AI 模型单价字段（增量 DDL，MySQL 8.0+）
-- 适用：已部署环境升级；全新安装执行 15_agent_model_sensitive.sql 即可（已含本文件变更）
-- 关联方案：《Token 计量与审批续跑深化方案》第 3.4 节（成本估算方案①）/ 分期 P5
-- 背景：Token 真实计量落地后需要折算成本，成本口径采用「模型单价配置化」——
--       单价直接落在 ai_model 上，随动态模型池刷新缓存到 AgentModelResolver，
--       由 AgentUsageEventListener 在日聚合时按 input/output 分别折算写入 agent_token_usage.cost。
-- 口径：单位为「元 / 千 token」，输入与输出分价（多数供应商输出价更高）；
--       未配置（0）表示只计量不计费，成本记 0，不阻断主流程。
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1. ai_model 增加输入/输出单价
-- ------------------------------------------------------------
ALTER TABLE `ai_model`
    ADD COLUMN `input_price`  DECIMAL(12, 6) NOT NULL DEFAULT 0 COMMENT '输入单价（元/千 token，0=未定价）' AFTER `capability`,
    ADD COLUMN `output_price` DECIMAL(12, 6) NOT NULL DEFAULT 0 COMMENT '输出单价（元/千 token，0=未定价）' AFTER `input_price`;

-- ------------------------------------------------------------
-- 2. 示例：按公开报价回填已启用模型（数值以供应商最新报价为准，此处仅作演示，可按需调整）
-- ------------------------------------------------------------
-- UPDATE `ai_model` SET `input_price` = 2.400000, `output_price` = 9.600000 WHERE `code` = 'qwen-max';
-- UPDATE `ai_model` SET `input_price` = 1.000000, `output_price` = 2.000000 WHERE `code` = 'deepseek-chat';

-- ------------------------------------------------------------
-- 3. 校验：两列应存在且默认 0
-- ------------------------------------------------------------
-- SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT FROM INFORMATION_SCHEMA.COLUMNS
--   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_model'
--     AND COLUMN_NAME IN ('input_price', 'output_price');
