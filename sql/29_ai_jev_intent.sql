-- ============================================================
-- Jev 判断模型集成（B1 意图路由，增量 DDL，MySQL 8.0+）
-- 关联方案：《AgentScope2.0-Jev判断模型集成方案》P0 首发项（第十四章）
-- 背景：taskType 缺省（auto）时服务端经 Jev Choice 结构化决策做一次性意图分类，
--       分类结果（含降级/粘滞复用）异步采样落本表，作为命中率/降级率/置信度
--       分布观测与阈值调优的数据基础；后续 P1 模型路由、P2 审批预判复用同表
--       （scene 区分），无需再建表。
-- 口径：非降级记录按 zhush.agentscope.jev.log-sample-rate 采样；degraded/sticky
--       记录全量保留（服务健康指标不能留采样盲区）；本表只写不读（观测用途），
--       不建业务索引之外的冗余。执行一次即可，重复执行幂等。
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1. Jev 决策记录表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_jev_decision_log` (
    `id`           BIGINT        NOT NULL COMMENT '主键ID（雪花算法）',
    `scene`        TINYINT       NOT NULL DEFAULT 1 COMMENT '决策场景：1-意图路由 2-模型路由(预留) 3-审批预判(预留)',
    `session_id`   VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '业务会话ID（新建会话时为空）',
    `agent_name`   VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '智能体名',
    `user_id`      BIGINT        NOT NULL DEFAULT 0 COMMENT '归属用户ID',
    `user_type`    VARCHAR(16)   NOT NULL DEFAULT 'admin' COMMENT '用户类型：admin-管理端 member-会员端',
    `question_type` VARCHAR(16)  NOT NULL DEFAULT '' COMMENT '问题类型：noul/choice/score',
    `answer`       VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '决策结果（choice=选项 code，noul=概率值）',
    `confidence`   DECIMAL(5,4)      NULL COMMENT '置信度（0~1；降级记录为 NULL）',
    `degraded`     TINYINT       NOT NULL DEFAULT 0 COMMENT '是否降级：0-正常 1=Jev 失败/不可用回落 chat',
    `sticky`       TINYINT       NOT NULL DEFAULT 0 COMMENT '是否粘滞复用：0-否 1=命中上轮分类未请求 Jev',
    `cost_ms`      INT           NOT NULL DEFAULT 0 COMMENT '决策耗时（毫秒，含超时等待）',
    `error_msg`    VARCHAR(512)  NOT NULL DEFAULT '' COMMENT '降级原因（degraded=1 时记录）',
    `create_by`    BIGINT            NULL COMMENT '创建人ID',
    `create_time`  DATETIME          NULL COMMENT '创建时间',
    `update_by`    BIGINT            NULL COMMENT '更新人ID',
    `update_time`  DATETIME          NULL COMMENT '更新时间',
    `is_delete`    TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_scene_time` (`scene`, `create_time`),
    KEY `idx_degraded` (`degraded`, `create_time`),
    KEY `idx_session_id` (`session_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Jev 判断模型决策记录表（观测/调优数据面）';

-- ------------------------------------------------------------
-- 2. 校验：表应存在
-- ------------------------------------------------------------
-- SELECT TABLE_NAME, TABLE_COMMENT FROM INFORMATION_SCHEMA.TABLES
--   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_jev_decision_log';
