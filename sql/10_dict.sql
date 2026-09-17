-- ============================================================
-- 数据字典 DDL（MySQL 8.0+）
-- 字符集：utf8mb4    存储引擎：InnoDB
-- 主键：雪花算法（应用层生成，BIGINT）
-- 公共字段：create_by / create_time / update_by / update_time / is_delete
--          与 com.zhush.common.entity.BaseEntity 对应
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 字典类型表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
    `id`          BIGINT       NOT NULL COMMENT '主键ID（雪花算法）',
    `name`        VARCHAR(128) NOT NULL COMMENT '字典名称（如 性别）',
    `type`        VARCHAR(128) NOT NULL COMMENT '字典类型编码（如 common_status，业务方按编码取数）',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `remark`      VARCHAR(512) NOT NULL DEFAULT '' COMMENT '备注',
    `create_by`   BIGINT           NULL COMMENT '创建人ID',
    `create_time` DATETIME         NULL COMMENT '创建时间',
    `update_by`   BIGINT           NULL COMMENT '更新人ID',
    `update_time` DATETIME         NULL COMMENT '更新时间',
    `is_delete`   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type` (`type`, `is_delete`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '字典类型表';

-- ------------------------------------------------------------
-- 字典数据表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
    `id`          BIGINT       NOT NULL COMMENT '主键ID（雪花算法）',
    `dict_type`   VARCHAR(128) NOT NULL COMMENT '字典类型编码（冗余存储，与 sys_dict_type.type 对应）',
    `label`       VARCHAR(128) NOT NULL COMMENT '字典标签（展示值，如 男）',
    `value`       VARCHAR(128) NOT NULL COMMENT '字典键值（存储值，如 1）',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `remark`      VARCHAR(512) NOT NULL DEFAULT '' COMMENT '备注',
    `create_by`   BIGINT           NULL COMMENT '创建人ID',
    `create_time` DATETIME         NULL COMMENT '创建时间',
    `update_by`   BIGINT           NULL COMMENT '更新人ID',
    `update_time` DATETIME         NULL COMMENT '更新时间',
    `is_delete`   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_value` (`dict_type`, `value`, `is_delete`),
    KEY `idx_dict_type` (`dict_type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '字典数据表';

-- ============================================================
-- 菜单权限（ID 约定：目录 1000 / 菜单 11xx / 按钮 菜单ID*10+序）
-- 幂等：先删旧数据再插入；超级管理员（super_admin）自带 * 通配权限，无需授权
-- ============================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (1000, 1100, 1101, 11001, 11002, 11003, 11004, 11011, 11012, 11013, 11014);
DELETE FROM `sys_menu` WHERE `id` IN (1000, 1100, 1101, 11001, 11002, 11003, 11004, 11011, 11012, 11013, 11014);

-- 系统管理目录
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`)
VALUES (1000, 0, '系统管理', 1, '/system', 1);

-- 字典类型
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (1100, 1000, '字典类型', 2, '/system/dict-type', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (11001, 1100, '字典类型查询', 3, 'system:dict-type:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (11002, 1100, '字典类型新增', 3, 'system:dict-type:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (11003, 1100, '字典类型修改', 3, 'system:dict-type:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (11004, 1100, '字典类型删除', 3, 'system:dict-type:delete', 4);

-- 字典数据
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (1101, 1000, '字典数据', 2, '/system/dict-data', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (11011, 1101, '字典数据查询', 3, 'system:dict-data:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (11012, 1101, '字典数据新增', 3, 'system:dict-data:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (11013, 1101, '字典数据修改', 3, 'system:dict-data:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (11014, 1101, '字典数据删除', 3, 'system:dict-data:delete', 4);

-- ============================================================
-- 初始化字典（通用状态 / 性别）
-- ============================================================
INSERT INTO `sys_dict_type` (`id`, `name`, `type`, `status`, `remark`)
VALUES (1, '通用状态', 'common_status', 1, '启用/禁用');
INSERT INTO `sys_dict_type` (`id`, `name`, `type`, `status`, `remark`)
VALUES (2, '性别', 'system_user_sex', 1, '用户性别');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `label`, `value`, `sort`, `status`)
VALUES (1, 'common_status', '启用', '1', 1, 1),
       (2, 'common_status', '禁用', '0', 2, 1),
       (3, 'system_user_sex', '男', '1', 1, 1),
       (4, 'system_user_sex', '女', '2', 2, 1),
       (5, 'system_user_sex', '未知', '0', 3, 1);

-- ============================================================
-- 联盟提现支付渠道字典（与 union_withdraw_apply.pay_channel 取值一一对应）
-- dict_type = union_pay_channel；与提现方式(withdraw_type)显式区分，微信可走 V2 或 V3
-- ============================================================
INSERT INTO `sys_dict_type` (`id`, `name`, `type`, `status`, `remark`)
VALUES (3, '提现支付渠道', 'union_pay_channel', 1,
        '提现出款实际使用的支付协议；与提现方式(withdraw_type)区分，微信可走 V2 或 V3');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `label`, `value`, `sort`, `status`)
VALUES (6, 'union_pay_channel', '微信V2（企业付款到银行卡）', 'WECHAT_V2', 1, 1),
       (7, 'union_pay_channel', '微信V3（商家转账）',        'WECHAT_V3', 2, 1),
       (8, 'union_pay_channel', '支付宝',                    'ALIPAY',    3, 1);
