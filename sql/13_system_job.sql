-- ============================================================
-- 定时任务管理模块（MySQL 8.0+）
-- 前置依赖：07_quartz.sql（Quartz InnoDB 16 张表，job-store-type=jdbc 时必须）
-- 字符集：utf8mb4    存储引擎：InnoDB
-- 主键：雪花算法（应用层生成，BIGINT）
-- 公共字段：create_by / create_time / update_by / update_time / is_delete
--          与 com.zhush.common.entity.BaseEntity 对应
-- 写入方：com.zhush.system.service.AdminJobService（任务定义）
--        com.zhush.system.service.JobLogService（执行记录）
--
-- 菜单 ID 规划（新增一级目录「系统监控」，与「系统管理」同级）：
--   2000        系统监控（目录）
--   2001        定时任务（菜单）   20011~20016 按钮权限点，20021~20022 调度日志（执行记录）按钮权限点
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 定时任务表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_job`;
CREATE TABLE `sys_job` (
    `id`              BIGINT        NOT NULL COMMENT '主键ID（雪花算法）',
    `job_name`        VARCHAR(64)   NOT NULL COMMENT '任务名称',
    `job_group`       VARCHAR(64)   NOT NULL DEFAULT 'DEFAULT' COMMENT '任务分组',
    `invoke_target`   VARCHAR(512)  NOT NULL COMMENT '调用目标：Spring Bean 方法，如 orderService.closeExpiredOrders()',
    `cron_expression` VARCHAR(128)  NOT NULL COMMENT 'Cron 表达式（Quartz 7 段格式）',
    `params`          VARCHAR(2048) NOT NULL DEFAULT '' COMMENT '任务参数（JSON，透传给调用目标方法）',
    `concurrent`      TINYINT       NOT NULL DEFAULT 1 COMMENT '是否允许并发执行：0-禁止 1-允许',
    `status`          TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：0-停用 1-启用（即启动/停止）',
    `remark`          VARCHAR(512)  NOT NULL DEFAULT '' COMMENT '备注',
    `create_by`       BIGINT            NULL COMMENT '创建人ID',
    `create_time`     DATETIME          NULL COMMENT '创建时间',
    `update_by`       BIGINT            NULL COMMENT '更新人ID',
    `update_time`     DATETIME          NULL COMMENT '更新时间',
    `is_delete`       TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name_group` (`job_name`, `job_group`, `is_delete`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '定时任务表';

-- ------------------------------------------------------------
-- 定时任务执行记录表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_job_log`;
CREATE TABLE `sys_job_log` (
    `id`              BIGINT        NOT NULL COMMENT '主键ID（雪花算法）',
    `job_id`          BIGINT        NOT NULL COMMENT '任务ID（sys_job.id，任务删除后记录保留）',
    `job_name`        VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '任务名称（冗余快照）',
    `job_group`       VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '任务分组（冗余快照）',
    `invoke_target`   VARCHAR(512)  NOT NULL DEFAULT '' COMMENT '调用目标（冗余快照）',
    `status`          TINYINT       NOT NULL DEFAULT 2 COMMENT '执行状态：0-失败 1-成功 2-运行中',
    `job_message`     VARCHAR(2048) NOT NULL DEFAULT '' COMMENT '执行消息（成功摘要 / 失败原因）',
    `exception_info`  TEXT              NULL COMMENT '异常堆栈（失败时记录）',
    `start_time`      DATETIME          NULL COMMENT '开始时间',
    `end_time`        DATETIME          NULL COMMENT '结束时间',
    `duration`        BIGINT            NULL COMMENT '耗时（毫秒）',
    `create_by`       BIGINT            NULL COMMENT '创建人ID',
    `create_time`     DATETIME          NULL COMMENT '创建时间（即记录时间）',
    `update_by`       BIGINT            NULL COMMENT '更新人ID',
    `update_time`     DATETIME          NULL COMMENT '更新时间',
    `is_delete`       TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_job_id` (`job_id`),
    KEY `idx_status` (`status`),
    KEY `idx_start_time` (`start_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '定时任务执行记录表';

-- ============================================================
-- 菜单权限（目录 2000 系统监控 / 菜单 2001、2002 / 按钮 菜单ID*10+序）
-- 幂等：先删旧数据再插入；超级管理员（super_admin）自带 * 通配权限，关联仅为显性展示
-- ============================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (2000, 2001, 2002, 20011, 20012, 20013, 20014, 20015, 20016, 20021, 20022);
DELETE FROM `sys_menu` WHERE `id` IN (2000, 2001, 2002, 20011, 20012, 20013, 20014, 20015, 20016, 20021, 20022);

-- 系统监控（一级目录，与系统管理同级；path 存相对路径段，后端逐级拼接）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `icon`, `sort`, `visible`, `status`)
VALUES (2000, 0, '系统监控', 1, 'monitor', 'lucide:activity', 90, 1, 1);

-- 页面菜单（component 对应前端 views 目录：monitor/job/index.vue）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
VALUES
    (2001, 2000, '定时任务', 2, 'job', '/monitor/job/index', 'lucide:clock', 1, 1, 1);

-- 按钮权限点（type=3，permission 与后端 @SaCheckPermission 一致）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
VALUES
    -- 定时任务
    (20011, 2001, '定时任务查询', 3, 'system:job:query',         '', '', '', 1, 1, 1),
    (20012, 2001, '定时任务新增', 3, 'system:job:create',        '', '', '', 2, 1, 1),
    (20013, 2001, '定时任务修改', 3, 'system:job:update',        '', '', '', 3, 1, 1),
    (20014, 2001, '定时任务删除', 3, 'system:job:delete',        '', '', '', 4, 1, 1),
    (20015, 2001, '启动/停止',    3, 'system:job:change-status', '', '', '', 5, 1, 1),
    (20016, 2001, '执行一次',     3, 'system:job:trigger',       '', '', '', 6, 1, 1),
    -- 调度日志（执行记录，挂在定时任务菜单下，作为操作列入口）
    (20021, 2001, '调度日志查询', 3, 'system:job-log:query',     '', '', '', 7, 1, 1),
    (20022, 2001, '调度日志删除', 3, 'system:job-log:delete',    '', '', '', 8, 1, 1);

-- 超级管理员（role_id=1）关联全部菜单与权限点
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
VALUES
    (20000, 1, 2000),
    (20001, 1, 2001),
    (20011, 1, 20011), (20012, 1, 20012), (20013, 1, 20013),
    (20014, 1, 20014), (20015, 1, 20015), (20016, 1, 20016),
    (20021, 1, 20021), (20022, 1, 20022);

-- ============================================================
-- 初始化演示数据（可选，用于联调/演示）
-- 依赖 zhush-module-system 中 com.zhush.system.job.DemoJobService（beanName=demoJobService）
-- 正式环境可删除本段，或改为真实业务 Bean 方法作为调用目标
-- ============================================================

-- 定时任务演示数据（status=1 启用，服务启动后将在调度器中注册并按 cron 触发）
INSERT INTO `sys_job` (`id`, `job_name`, `job_group`, `invoke_target`, `cron_expression`, `params`, `concurrent`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `is_delete`)
VALUES
    (300000000000000001, '演示任务',   'DEFAULT', 'demoJobService.demoTask()',         '*/30 * * * * ?', '',                                      0, 1, '无参示例：每 30 秒执行，便于观察调度日志', 0, NOW(), 0, NOW(), 0),
    (300000000000000002, '清理过期数据', 'DEFAULT', 'demoJobService.cleanExpiredData()', '0 0 2 * * ?',    '',                                      0, 1, '无参示例：每天凌晨 2 点清理',           0, NOW(), 0, NOW(), 0),
    (300000000000000003, '处理订单',    'DEFAULT', 'demoJobService.processOrder()',     '0 0/5 * * * ?',  '{"orderNo":"20260902001","retryTimes":3}', 0, 1, '带参示例：每 5 分钟处理（多形参按形参名取值）', 0, NOW(), 0, NOW(), 0);

-- 初始写入时无 schedule 记录，需保证上述任务已注册；此处不重复调度，交由 JobStartupRunner 启动补漏

-- 执行记录演示数据（用来查看调度日志；正式运行时由任务执行自动生成）
INSERT INTO `sys_job_log` (`id`, `job_id`, `job_name`, `job_group`, `invoke_target`, `status`, `job_message`, `exception_info`, `start_time`, `end_time`, `duration`, `create_by`, `create_time`, `update_by`, `update_time`, `is_delete`)
VALUES
    (310000000000000001, 300000000000000001, '演示任务', 'DEFAULT', 'demoJobService.demoTask()',         1, '执行成功', NULL, NOW(), NOW(), 12,  0, NOW(), 0, NOW(), 0),
    (310000000000000002, 300000000000000003, '处理订单', 'DEFAULT', 'demoJobService.processOrder()',    0, '业务处理异常：订单不存在', 'com.zhush.common.exception.ServiceException: 订单 20260902001 不存在\n\tat com.zhush.system.job.DemoJobService.processOrder(DemoJobService.java:45)\n\tat com.zhush.system.job.JobInvokeTargetParser.invoke(JobInvokeTargetParser.java:64)', NOW(), NOW(), 3, 0, NOW(), 0, NOW(), 0);
