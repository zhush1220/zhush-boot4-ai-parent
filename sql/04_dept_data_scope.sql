-- ============================================================
-- 部门表 + 角色数据范围扩展 DDL（MySQL 8.0+）
-- 字符集：utf8mb4    存储引擎：InnoDB
-- 主键：雪花算法（应用层生成，BIGINT）
-- 公共字段：create_by / create_time / update_by / update_time / is_delete
--          与 com.zhush.common.entity.BaseEntity 对应
-- 配套功能：zhush-spring-boot-starter-biz-data-permission 数据权限
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 部门表（树形结构，根部门 parent_id = 0）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
    `id`             BIGINT       NOT NULL COMMENT '主键ID（雪花算法）',
    `parent_id`      BIGINT       NOT NULL DEFAULT 0 COMMENT '父部门ID（根为0）',
    `name`           VARCHAR(64)  NOT NULL COMMENT '部门名称',
    `sort`           INT          NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `leader_user_id` BIGINT           NULL COMMENT '负责人用户ID',
    `phone`          VARCHAR(20)  NOT NULL DEFAULT '' COMMENT '联系电话',
    `email`          VARCHAR(128) NOT NULL DEFAULT '' COMMENT '邮箱',
    `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `create_by`      BIGINT           NULL COMMENT '创建人ID',
    `create_time`    DATETIME         NULL COMMENT '创建时间',
    `update_by`      BIGINT           NULL COMMENT '更新人ID',
    `update_time`    DATETIME         NULL COMMENT '更新时间',
    `is_delete`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '部门表';

-- ------------------------------------------------------------
-- 角色表扩展：自定义数据范围的部门ID集合（data_scope = 5 时生效，逗号分隔）
-- ------------------------------------------------------------
ALTER TABLE `sys_role`
    ADD COLUMN `data_scope_dept_ids` VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '自定义数据范围的部门ID集合（逗号分隔，data_scope=5 时生效）' AFTER `data_scope`;

-- ------------------------------------------------------------
-- 初始化根部门（可按需修改名称）
-- ------------------------------------------------------------
INSERT INTO `sys_dept` (`id`, `parent_id`, `name`, `sort`, `status`, `create_time`)
VALUES (1, 0, '总公司', 0, 1, NOW());
