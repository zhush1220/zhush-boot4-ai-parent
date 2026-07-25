-- ============================================================
-- 操作日志表 DDL（MySQL 8.0+）
-- 字符集：utf8mb4    存储引擎：InnoDB
-- 主键：雪花算法（应用层生成，BIGINT）
-- 公共字段：create_by / create_time / update_by / update_time / is_delete
--          与 com.zhush.common.entity.BaseEntity 对应
-- 写入方：com.zhush.system.service.OperateLogService（@OperateLog 注解 + AOP 采集）
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 操作日志表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_operate_log`;
CREATE TABLE `sys_operate_log` (
    `id`               BIGINT        NOT NULL COMMENT '主键ID（雪花算法）',
    `trace_id`         VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '链路追踪ID（预留）',
    `user_id`          BIGINT            NULL COMMENT '操作人ID（未登录为 NULL）',
    `user_type`        VARCHAR(16)   NOT NULL DEFAULT '' COMMENT '操作人类型：admin-管理端 member-会员端',
    `module`           VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '所属模块（如 用户管理）',
    `name`             VARCHAR(128)  NOT NULL DEFAULT '' COMMENT '操作名称（如 新增管理员）',
    `type`             TINYINT       NOT NULL DEFAULT 0 COMMENT '操作类型：0-其它 1-查询 2-新增 3-修改 4-删除 5-导出 6-导入 7-授权',
    `request_method`   VARCHAR(16)   NOT NULL DEFAULT '' COMMENT '请求方式（GET/POST/PUT/DELETE）',
    `request_url`      VARCHAR(256)  NOT NULL DEFAULT '' COMMENT '请求地址',
    `user_ip`          VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '操作人IP',
    `user_agent`       VARCHAR(512)  NOT NULL DEFAULT '' COMMENT '浏览器UA',
    `java_method`      VARCHAR(256)  NOT NULL DEFAULT '' COMMENT '目标Java方法（类全名.方法名）',
    `java_method_args` VARCHAR(2000) NOT NULL DEFAULT '' COMMENT '方法入参（JSON，超长截断）',
    `start_time`       DATETIME          NULL COMMENT '操作开始时间',
    `duration`         INT           NOT NULL DEFAULT 0 COMMENT '执行耗时（毫秒）',
    `result_code`      INT           NOT NULL DEFAULT 0 COMMENT '结果码：0-成功，其余为错误码',
    `result_msg`       VARCHAR(512)  NOT NULL DEFAULT '' COMMENT '结果提示信息（失败时为异常信息）',
    `result_data`      VARCHAR(4000) NOT NULL DEFAULT '' COMMENT '返回结果（JSON，超长截断）',
    `create_by`        BIGINT            NULL COMMENT '创建人ID',
    `create_time`      DATETIME          NULL COMMENT '创建时间',
    `update_by`        BIGINT            NULL COMMENT '更新人ID',
    `update_time`      DATETIME          NULL COMMENT '更新时间',
    `is_delete`        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_type`, `user_id`),
    KEY `idx_module` (`module`),
    KEY `idx_start_time` (`start_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '操作日志表';

-- ------------------------------------------------------------
-- 菜单（操作日志查询权限，挂在系统管理目录下按需调整 parent_id）
-- ------------------------------------------------------------
-- INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `sort`)
-- VALUES (企业自行分配ID, 系统管理目录ID, '操作日志', 2, 'system:operate-log:query', '/system/operate-log', 99);
