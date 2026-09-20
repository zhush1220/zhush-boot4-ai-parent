-- ============================================================
-- SQL 审计日志表 DDL（MySQL 8.0+）
-- 字符集：utf8mb4    存储引擎：InnoDB
-- 主键：雪花算法（应用层生成，BIGINT）
-- 公共字段：create_by / create_time / update_by / update_time / is_delete
--          与 com.zhush.common.entity.BaseEntity 对应
-- 写入方：MyBatis-Flex AuditManager → 框架层 SqlAuditMessageReporter（JdbcTemplate 批量，绕开拦截器防递归）
-- 读取方：com.zhush.system.vo.SysSqlAuditLogMapper + SqlAuditLogService（管理端分页/详情）
-- 说明：日志表只增不改，update_* / is_delete 保留但通常不更新（删除走逻辑删除）
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- SQL 审计日志表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_sql_audit_log`;
    CREATE TABLE `sys_sql_audit_log` (
    `id`            BIGINT        NOT NULL COMMENT '主键ID（雪花算法）',
    `trace_id`      VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '链路追踪ID',
    `platform`      VARCHAR(16)   NOT NULL DEFAULT '' COMMENT '平台：admin-管理端 member-会员端',
    `module`        VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '所属模块',
    `url`           VARCHAR(256)  NOT NULL DEFAULT '' COMMENT '请求URL',
    `biz_id`        VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '业务ID（可选）',
    `user`          VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '操作人（用户ID/用户名）',
    `user_ip`       VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '用户IP',
    `host_ip`       VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '主机IP',
    `ds_name`       VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '数据源名称',
    `stmt_id`       VARCHAR(256)  NOT NULL DEFAULT '' COMMENT 'Mapper 语句全限定ID（com.xxx.Mapper.method）',
    `sql`           TEXT          NOT NULL COMMENT '原始SQL文本，超长截断',
    `sql_params`    TEXT          NOT NULL COMMENT 'SQL参数（JSON），超长截断',
    `full_sql`      TEXT          NOT NULL COMMENT '完整SQL（含内联参数），超长截断',
    `query_count`   INT           NOT NULL DEFAULT 0  COMMENT '查询返回条数',
    `query_time`    DATETIME          NULL COMMENT '查询发生时间（毫秒时间戳转）',
    `elapsed_time`  BIGINT        NOT NULL DEFAULT 0  COMMENT '执行耗时（毫秒）',
    `metas`         TEXT          NOT NULL COMMENT '扩展元信息（JSON），超长截断',
    `create_by`     BIGINT            NULL COMMENT '创建人ID',
    `create_time`   DATETIME          NULL COMMENT '创建时间',
    `update_by`     BIGINT            NULL COMMENT '更新人ID（日志表保留，不更新）',
    `update_time`   DATETIME          NULL COMMENT '更新时间（日志表保留，不更新）',
    `is_delete`     TINYINT       NOT NULL DEFAULT 0  COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_platform_user` (`platform`, `user`),
    KEY `idx_stmt_id` (`stmt_id`),
    KEY `idx_ds_name` (`ds_name`),
    KEY `idx_elapsed_time` (`elapsed_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'SQL审计日志表';

-- ------------------------------------------------------------
-- 菜单与按钮权限点（父目录：系统管理 id=100，见 12_system_menu.sql）
-- 超管（super_admin, role_id=1）自带 * 通配权限，此处仍补角色关联便于演示
-- ------------------------------------------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (1103, 11031, 11032);
DELETE FROM `sys_menu` WHERE `id` IN (1103, 11031, 11032);

-- SQL审计（菜单，component 对应前端 views/system/sql-audit/index.vue）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
VALUES
    (1103, 100, 'SQL审计', 2, '', 'sql-audit', '/system/sql-audit/index', 'lucide:database', 6, 1, 1);

INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
VALUES
    (11031, 1103, 'SQL审计查询', 3, 'system:sql-audit:query', '', '', '', 1, 1, 1),
    (11032, 1103, 'SQL审计删除', 3, 'system:sql-audit:delete', '', '', '', 2, 1, 1);

-- 超级管理员（role_id=1）关联 SQL审计菜单与权限点
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
VALUES
    (1103, 1, 1103),
    (11031, 1, 11031),
    (11032, 1, 11032);
