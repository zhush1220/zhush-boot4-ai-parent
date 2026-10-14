-- ============================================================
-- 数据权限页面化配置：三层元数据表 + 数据资源管理菜单种子（MySQL 8.0+）
-- 关联代码：sys_data_resource / sys_data_field / sys_role_data_resource 三个实体
--          DataResourceMetadataSyncer（扫描 @DataPermission/@FieldPermission 登记）
--          AdminDataResourceController（system:data-resource:query/sync/update）
-- 依赖：01_admin_rbac.sql、04_dept_data_scope.sql、12_system_menu.sql、
--       31_data_permission_activation.sql（复用其 116~118 列级权限点）
-- 幂等：建表用 DROP IF EXISTS + CREATE；菜单/授权用先删后插（与 12/31 风格一致）
-- 主键：雪花算法（应用层生成，BIGINT）；公共字段对应 com.zhush.common.entity.BaseEntity
-- 回滚：见文件末尾「回滚段」（DELETE + 恢复说明）
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1. 数据资源登记表（纳入行级+列级管控的业务对象元数据）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_data_resource`;
CREATE TABLE `sys_data_resource` (
    `id`             BIGINT       NOT NULL COMMENT '主键ID（雪花算法）',
    `code`           VARCHAR(128) NOT NULL COMMENT '资源编码（全局唯一，如 system:user，对应 @DataPermission.resource）',
    `name`           VARCHAR(64)  NOT NULL COMMENT '资源名称（页面展示，如"管理员用户"）',
    `module`         VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '所属模块（如 system、union）',
    `table_name`     VARCHAR(128) NOT NULL DEFAULT '' COMMENT '业务表名（如 sys_admin_user）',
    `dept_column`    VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '部门归属列名（镜像 @DataPermission.deptColumn）',
    `user_column`    VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '用户归属列名（镜像 @DataPermission.userColumn）',
    `vo_class`       VARCHAR(256) NOT NULL DEFAULT '' COMMENT '返回 VO 类全限定名（列级字段扫描依据）',
    `filter_columns` VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '自定义行级条件可引用列白名单（逗号分隔，dept/user 列之外）',
    `row_enabled`    TINYINT      NOT NULL DEFAULT 1 COMMENT '是否纳入行级管控：0-否 1-是',
    `field_enabled`  TINYINT      NOT NULL DEFAULT 1 COMMENT '是否纳入列级管控：0-否 1-是',
    `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `create_by`      BIGINT           NULL COMMENT '创建人ID',
    `create_time`    DATETIME         NULL COMMENT '创建时间',
    `update_by`      BIGINT           NULL COMMENT '更新人ID',
    `update_time`    DATETIME         NULL COMMENT '更新时间',
    `is_delete`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '数据资源登记表';

-- ------------------------------------------------------------
-- 2. 受控字段元数据表（列级数据权限的可授权字段清单，权限点复用 sys_menu）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_data_field`;
CREATE TABLE `sys_data_field` (
    `id`               BIGINT       NOT NULL COMMENT '主键ID（雪花算法）',
    `resource_id`      BIGINT       NOT NULL COMMENT '所属资源ID（sys_data_resource.id）',
    `resource_code`    VARCHAR(128) NOT NULL COMMENT '资源编码（冗余，便于按编码查询）',
    `field_path`       VARCHAR(128) NOT NULL COMMENT 'VO 字段路径（如 mobile、loginIp）',
    `label`            VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '字段中文名（页面展示）',
    `permission_code`  VARCHAR(128) NOT NULL COMMENT '权限标识（如 system:user:mobile，对应 sys_menu.permission）',
    `menu_id`          BIGINT           NULL COMMENT '关联的 sys_menu 按钮ID（授权即分配该菜单）',
    `default_strategy` VARCHAR(16)  NOT NULL DEFAULT 'CLEAR' COMMENT '默认策略：CLEAR-置空 MASK-脱敏（镜像 @FieldPermission.strategy）',
    `mask_value`       VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '脱敏占位值（镜像 @FieldPermission.maskValue）',
    `status`           TINYINT      NOT NULL DEFAULT 1 COMMENT '是否纳入列级管控：0-否（放行） 1-是（受控）',
    `create_by`        BIGINT           NULL COMMENT '创建人ID',
    `create_time`      DATETIME         NULL COMMENT '创建时间',
    `update_by`        BIGINT           NULL COMMENT '更新人ID',
    `update_time`      DATETIME         NULL COMMENT '更新时间',
    `is_delete`        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_resource_field` (`resource_code`, `field_path`),
    KEY `idx_permission_code` (`permission_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '受控字段元数据表';

-- ------------------------------------------------------------
-- 3. 角色 × 业务对象行级数据范围覆盖规则表
--    scope_type：1-全部 2-本部门 3-本部门及以下 4-仅本人 5-自定义部门 6-自定义条件
--    与 sys_role.data_scope（全局默认）配合：命中本表则覆盖，未配置回落默认
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role_data_resource`;
CREATE TABLE `sys_role_data_resource` (
    `id`             BIGINT       NOT NULL COMMENT '主键ID（雪花算法）',
    `role_id`        BIGINT       NOT NULL COMMENT '角色ID',
    `resource_code`  VARCHAR(128) NOT NULL COMMENT '资源编码（sys_data_resource.code）',
    `scope_type`     TINYINT      NOT NULL COMMENT '数据范围类型：1-全部 2-本部门 3-本部门及以下 4-仅本人 5-自定义部门 6-自定义条件',
    `dept_ids`       VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '自定义部门ID集合（逗号分隔，scope_type=5 生效；前端数组↔后端逗号串）',
    `condition_json` TEXT             NULL COMMENT '自定义条件 JSON（scope_type=6 生效，字段/操作符白名单校验后编译为参数化条件）',
    `create_by`      BIGINT           NULL COMMENT '创建人ID',
    `create_time`    DATETIME         NULL COMMENT '创建时间',
    `update_by`      BIGINT           NULL COMMENT '更新人ID',
    `update_time`    DATETIME         NULL COMMENT '更新时间',
    `is_delete`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_resource` (`role_id`, `resource_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色数据资源行级规则表';

-- ------------------------------------------------------------
-- 4. 数据资源管理菜单 + 按钮权限点种子
--    ID 规划（延续 12_system_menu.sql）：
--      106        数据资源管理（页面，parent=100 系统管理）
--      160 ~ 162  数据资源管理按钮权限点
--    角色数据权限配置页复用角色管理既有按钮（120 query / 124 assign-menu / 125 assign-data-scope），
--    因此本脚本不再新增角色侧权限点。
-- ------------------------------------------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (106, 160, 161, 162);
DELETE FROM `sys_menu` WHERE `id` IN (106, 160, 161, 162);

INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
VALUES
    (106, 100, '数据资源管理', 2, '', 'data-resource', '/system/data-resource/index', 'lucide:database', 6, 1, 1),
    (160, 106, '资源查询',     3, 'system:data-resource:query',  '', '', '', 1, 1, 1),
    (161, 106, '元数据同步',   3, 'system:data-resource:sync',   '', '', '', 2, 1, 1),
    (162, 106, '资源编辑',     3, 'system:data-resource:update', '', '', '', 3, 1, 1);

-- 超级管理员（role_id=1，权限标识 * 通配）关联，显式记录便于审计
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
VALUES
    (106, 1, 106),
    (160, 1, 160), (161, 1, 161), (162, 1, 162);

-- ------------------------------------------------------------
-- 5. 初始化系统内置资源（管理员用户），字段映射复用 31 脚本的 116~118 权限点
--    后续资源由 DataResourceMetadataSyncer 扫描注解自动登记，此处仅种子一条便于页面开箱可见
--    固定 id：资源 3001；字段 3101~3103（避开雪花区间，纯种子）
-- ------------------------------------------------------------
DELETE FROM `sys_data_field` WHERE `id` IN (3101, 3102, 3103);
DELETE FROM `sys_data_resource` WHERE `id` = 3001;

INSERT INTO `sys_data_resource`
    (`id`, `code`, `name`, `module`, `table_name`, `dept_column`, `user_column`, `vo_class`, `filter_columns`, `row_enabled`, `field_enabled`, `status`, `create_time`)
VALUES
    (3001, 'system:user', '管理员用户', 'system', 'sys_admin_user', 'dept_id', 'id',
     'com.zhush.system.vo.AdminUserVO', 'username,create_time', 1, 1, 1, NOW());

INSERT INTO `sys_data_field`
    (`id`, `resource_id`, `resource_code`, `field_path`, `label`, `permission_code`, `menu_id`, `default_strategy`, `mask_value`, `status`, `create_time`)
VALUES
    (3101, 3001, 'system:user', 'mobile',   '手机号',   'system:user:mobile',   116, 'CLEAR', '', 1, NOW()),
    (3102, 3001, 'system:user', 'email',    '邮箱',     'system:user:email',    117, 'CLEAR', '', 1, NOW()),
    (3103, 3001, 'system:user', 'loginIp',  '登录IP',   'system:user:login-ip', 118, 'CLEAR', '', 1, NOW());

-- ------------------------------------------------------------
-- 回滚段（如需撤销本脚本，按下列顺序执行；DELETE + 幂等，与建表段对应）
-- ------------------------------------------------------------
-- DELETE FROM `sys_role_data_resource` WHERE 1 = 1;
-- DELETE FROM `sys_data_field` WHERE `id` IN (3101, 3102, 3103);
-- DELETE FROM `sys_data_resource` WHERE `id` = 3001;
-- DELETE FROM `sys_role_menu` WHERE `menu_id` IN (106, 160, 161, 162);
-- DELETE FROM `sys_menu` WHERE `id` IN (106, 160, 161, 162);
-- DROP TABLE IF EXISTS `sys_role_data_resource`;
-- DROP TABLE IF EXISTS `sys_data_field`;
-- DROP TABLE IF EXISTS `sys_data_resource`;
-- 说明：回滚仅移除"页面化配置"能力，不影响 31 脚本登记的列级权限点，
--       也不改变 sys_role.data_scope 的默认行为（存量角色仍为 1-全部数据）。
-- ------------------------------------------------------------
