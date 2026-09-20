-- ============================================================
-- 通知公告表审计列约束修复（增量 DDL，MySQL 8.0+）
-- 适用：已部署环境升级；全新安装执行 14_notice.sql 即可（已含本文件变更）
-- 背景：sys_notice 的 create_by / create_time / update_by / update_time 被建成 NOT NULL 且无默认值，
--       而插入时 GlobalInsertListener 只回填 create_by / create_time（update_* 由更新时回填），
--       MyBatis-Flex insert 又会显式写入全部列，于是「新增公告」直接失败：
--       java.sql.SQLIntegrityConstraintViolationException: Column 'update_by' cannot be null
--       （全库仅 sys_notice 如此，其余表审计列均为可空）
-- 变更：四列放开为可空，与 01_admin_rbac.sql 等表的审计列约定一致
-- 注意：仓库根目录 sql/zhush_union*.sql 为历史导出快照，若用它重建库需再执行本文件
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1. sys_notice 审计列改为可空（不影响 idx_notice_create_time 索引与既有数据）
-- ------------------------------------------------------------
ALTER TABLE `sys_notice`
    MODIFY COLUMN `create_by`   BIGINT   NULL COMMENT '创建人ID',
    MODIFY COLUMN `create_time` DATETIME NULL COMMENT '创建时间',
    MODIFY COLUMN `update_by`   BIGINT   NULL COMMENT '更新人ID',
    MODIFY COLUMN `update_time` DATETIME NULL COMMENT '更新时间';

-- ------------------------------------------------------------
-- 2. 校验：四列应为 NULLABLE = YES
-- ------------------------------------------------------------
-- SELECT COLUMN_NAME, IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS
--   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_notice'
--     AND COLUMN_NAME IN ('create_by', 'create_time', 'update_by', 'update_time');
