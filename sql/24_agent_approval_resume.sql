-- ============================================================
-- 审批续跑字段（增量 DDL，MySQL 8.0+）
-- 适用：已部署环境升级；全新安装执行 13_agent_ai.sql 即可（已含本文件变更）
-- 关联方案：《Token 计量与审批续跑深化方案》第 4.3 节 / 分期 P0、P6
-- 背景：审批续跑（路线 A/B）需要三项状态载体：
--       reply_id —— 路线 B 关联 AgentScope「require-confirm」本轮标识，恢复时按此喂回 ConfirmResult；
--       resumed  —— 续跑幂等位，一次审批仅续跑一次（approve 后 CAS 0→1），防止重复执行敏感工具；
--       resume_time —— 实际续跑触发时间，供审计与排查。
-- 口径：均带默认值，存量行回填为「未关联 / 未续跑 / 无续跑时间」，不影响历史数据。
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1. agent_approval 增加续跑关联与幂等列
-- ------------------------------------------------------------
ALTER TABLE `agent_approval`
    ADD COLUMN `reply_id`    VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'AgentScope require-confirm replyId（路线B恢复用，A 留空）' AFTER `expire_time`,
    ADD COLUMN `resumed`     TINYINT     NOT NULL DEFAULT 0 COMMENT '是否已续跑：0-未 1-已（幂等，一次审批仅续跑一次）' AFTER `reply_id`,
    ADD COLUMN `resume_time` DATETIME        NULL COMMENT '续跑时间' AFTER `resumed`;

-- ------------------------------------------------------------
-- 2. 校验：三列应存在
-- ------------------------------------------------------------
-- SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT FROM INFORMATION_SCHEMA.COLUMNS
--   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'agent_approval'
--     AND COLUMN_NAME IN ('reply_id', 'resumed', 'resume_time');
