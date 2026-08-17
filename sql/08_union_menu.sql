-- ============================================================
-- union 联盟推广：菜单 + 按钮权限初始化数据（MySQL 8.0+）
-- 依赖：01_admin_rbac.sql（sys_menu / sys_role_menu，角色 1=超级管理员）
-- ID 约定：2000=联盟推广目录；2100+=各资源菜单；按钮=菜单ID*10+序
-- 幂等：重复执行先清理 2000~2999 区间旧数据
-- 路由 path / 组件路径仅为占位，按前端工程实际调整
-- ============================================================

SET NAMES utf8mb4;

DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 2000 AND 2999;
DELETE FROM `sys_menu` WHERE `id` BETWEEN 2000 AND 2999;

-- 一级目录：联盟推广
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`)
VALUES (2000, 0, '联盟推广', 1, '/union', 20);

-- 账户资金流水
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2100, 2000, '账户资金流水', 2, '/union/account-transaction', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21001, 2100, '账户资金流水查询', 3, 'union:account-transaction:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21002, 2100, '账户资金流水新增', 3, 'union:account-transaction:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21003, 2100, '账户资金流水修改', 3, 'union:account-transaction:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21004, 2100, '账户资金流水删除', 3, 'union:account-transaction:delete', 4);

-- 平台API调用日志
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2101, 2000, '平台API调用日志', 2, '/union/api-call-log', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21011, 2101, '平台API调用日志查询', 3, 'union:api-call-log:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21012, 2101, '平台API调用日志新增', 3, 'union:api-call-log:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21013, 2101, '平台API调用日志修改', 3, 'union:api-call-log:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21014, 2101, '平台API调用日志删除', 3, 'union:api-call-log:delete', 4);

-- 自动审核配置
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2102, 2000, '自动审核配置', 2, '/union/auto-audit-config', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21021, 2102, '自动审核配置查询', 3, 'union:auto-audit-config:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21022, 2102, '自动审核配置新增', 3, 'union:auto-audit-config:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21023, 2102, '自动审核配置修改', 3, 'union:auto-audit-config:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21024, 2102, '自动审核配置删除', 3, 'union:auto-audit-config:delete', 4);

-- 商品分类
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2103, 2000, '商品分类', 2, '/union/category', 4);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21031, 2103, '商品分类查询', 3, 'union:category:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21032, 2103, '商品分类新增', 3, 'union:category:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21033, 2103, '商品分类修改', 3, 'union:category:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21034, 2103, '商品分类删除', 3, 'union:category:delete', 4);

-- 推广链接点击明细
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2104, 2000, '推广链接点击明细', 2, '/union/click-tracking', 5);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21041, 2104, '推广链接点击明细查询', 3, 'union:click-tracking:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21042, 2104, '推广链接点击明细新增', 3, 'union:click-tracking:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21043, 2104, '推广链接点击明细修改', 3, 'union:click-tracking:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21044, 2104, '推广链接点击明细删除', 3, 'union:click-tracking:delete', 4);

-- 佣金调整记录
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2105, 2000, '佣金调整记录', 2, '/union/commission-adjustment', 6);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21051, 2105, '佣金调整记录查询', 3, 'union:commission-adjustment:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21052, 2105, '佣金调整记录新增', 3, 'union:commission-adjustment:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21053, 2105, '佣金调整记录修改', 3, 'union:commission-adjustment:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21054, 2105, '佣金调整记录删除', 3, 'union:commission-adjustment:delete', 4);

-- 佣金分配明细
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2106, 2000, '佣金分配明细', 2, '/union/commission-distribution', 7);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21061, 2106, '佣金分配明细查询', 3, 'union:commission-distribution:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21062, 2106, '佣金分配明细新增', 3, 'union:commission-distribution:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21063, 2106, '佣金分配明细修改', 3, 'union:commission-distribution:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21064, 2106, '佣金分配明细删除', 3, 'union:commission-distribution:delete', 4);

-- 佣金记录
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2107, 2000, '佣金记录', 2, '/union/commission-record', 8);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21071, 2107, '佣金记录查询', 3, 'union:commission-record:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21072, 2107, '佣金记录新增', 3, 'union:commission-record:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21073, 2107, '佣金记录修改', 3, 'union:commission-record:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21074, 2107, '佣金记录删除', 3, 'union:commission-record:delete', 4);

-- 佣金分配规则配置
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2108, 2000, '佣金分配规则配置', 2, '/union/commission-rule', 9);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21081, 2108, '佣金分配规则配置查询', 3, 'union:commission-rule:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21082, 2108, '佣金分配规则配置新增', 3, 'union:commission-rule:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21083, 2108, '佣金分配规则配置修改', 3, 'union:commission-rule:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21084, 2108, '佣金分配规则配置删除', 3, 'union:commission-rule:delete', 4);

-- 外部推广者账户
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2109, 2000, '外部推广者账户', 2, '/union/external-account', 10);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21091, 2109, '外部推广者账户查询', 3, 'union:external-account:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21092, 2109, '外部推广者账户新增', 3, 'union:external-account:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21093, 2109, '外部推广者账户修改', 3, 'union:external-account:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21094, 2109, '外部推广者账户删除', 3, 'union:external-account:delete', 4);

-- 外部推广者佣金明细
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2110, 2000, '外部推广者佣金明细', 2, '/union/external-commission-detail', 11);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21101, 2110, '外部推广者佣金明细查询', 3, 'union:external-commission-detail:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21102, 2110, '外部推广者佣金明细新增', 3, 'union:external-commission-detail:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21103, 2110, '外部推广者佣金明细修改', 3, 'union:external-commission-detail:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21104, 2110, '外部推广者佣金明细删除', 3, 'union:external-commission-detail:delete', 4);

-- 外部推广者待结算佣金
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2111, 2000, '外部推广者待结算佣金', 2, '/union/external-commission-pending', 12);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21111, 2111, '外部推广者待结算佣金查询', 3, 'union:external-commission-pending:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21112, 2111, '外部推广者待结算佣金新增', 3, 'union:external-commission-pending:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21113, 2111, '外部推广者待结算佣金修改', 3, 'union:external-commission-pending:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21114, 2111, '外部推广者待结算佣金删除', 3, 'union:external-commission-pending:delete', 4);

-- 外部推广者/影子推广者
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2112, 2000, '外部推广者/影子推广者', 2, '/union/external-promoter', 13);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21121, 2112, '外部推广者/影子推广者查询', 3, 'union:external-promoter:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21122, 2112, '外部推广者/影子推广者新增', 3, 'union:external-promoter:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21123, 2112, '外部推广者/影子推广者修改', 3, 'union:external-promoter:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21124, 2112, '外部推广者/影子推广者删除', 3, 'union:external-promoter:delete', 4);

-- 外部推广者资金流水
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2113, 2000, '外部推广者资金流水', 2, '/union/external-transaction', 14);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21131, 2113, '外部推广者资金流水查询', 3, 'union:external-transaction:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21132, 2113, '外部推广者资金流水新增', 3, 'union:external-transaction:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21133, 2113, '外部推广者资金流水修改', 3, 'union:external-transaction:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21134, 2113, '外部推广者资金流水删除', 3, 'union:external-transaction:delete', 4);

-- 外部推广者提现申请
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2114, 2000, '外部推广者提现申请', 2, '/union/external-withdraw', 15);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21141, 2114, '外部推广者提现申请查询', 3, 'union:external-withdraw:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21142, 2114, '外部推广者提现申请新增', 3, 'union:external-withdraw:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21143, 2114, '外部推广者提现申请修改', 3, 'union:external-withdraw:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21144, 2114, '外部推广者提现申请删除', 3, 'union:external-withdraw:delete', 4);

-- 层级佣金配置
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2115, 2000, '层级佣金配置', 2, '/union/level-config', 16);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21151, 2115, '层级佣金配置查询', 3, 'union:level-config:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21152, 2115, '层级佣金配置新增', 3, 'union:level-config:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21153, 2115, '层级佣金配置修改', 3, 'union:level-config:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21154, 2115, '层级佣金配置删除', 3, 'union:level-config:delete', 4);

-- 转链配置
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2116, 2000, '转链配置', 2, '/union/link-config', 17);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21161, 2116, '转链配置查询', 3, 'union:link-config:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21162, 2116, '转链配置新增', 3, 'union:link-config:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21163, 2116, '转链配置修改', 3, 'union:link-config:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21164, 2116, '转链配置删除', 3, 'union:link-config:delete', 4);

-- 转链请求日志
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2117, 2000, '转链请求日志', 2, '/union/link-request-log', 18);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21171, 2117, '转链请求日志查询', 3, 'union:link-request-log:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21172, 2117, '转链请求日志新增', 3, 'union:link-request-log:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21173, 2117, '转链请求日志修改', 3, 'union:link-request-log:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21174, 2117, '转链请求日志删除', 3, 'union:link-request-log:delete', 4);

-- 推广者账户
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2118, 2000, '推广者账户', 2, '/union/member-account', 19);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21181, 2118, '推广者账户查询', 3, 'union:member-account:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21182, 2118, '推广者账户新增', 3, 'union:member-account:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21183, 2118, '推广者账户修改', 3, 'union:member-account:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21184, 2118, '推广者账户删除', 3, 'union:member-account:delete', 4);

-- 推广者设备信息
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2119, 2000, '推广者设备信息', 2, '/union/member-device', 20);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21191, 2119, '推广者设备信息查询', 3, 'union:member-device:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21192, 2119, '推广者设备信息新增', 3, 'union:member-device:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21193, 2119, '推广者设备信息修改', 3, 'union:member-device:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21194, 2119, '推广者设备信息删除', 3, 'union:member-device:delete', 4);

-- 推广者等级配置
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2120, 2000, '推广者等级配置', 2, '/union/member-level', 21);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21201, 2120, '推广者等级配置查询', 3, 'union:member-level:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21202, 2120, '推广者等级配置新增', 3, 'union:member-level:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21203, 2120, '推广者等级配置修改', 3, 'union:member-level:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21204, 2120, '推广者等级配置删除', 3, 'union:member-level:delete', 4);

-- 推广者关系
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2121, 2000, '推广者关系', 2, '/union/member-relation', 22);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21211, 2121, '推广者关系查询', 3, 'union:member-relation:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21212, 2121, '推广者关系新增', 3, 'union:member-relation:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21213, 2121, '推广者关系修改', 3, 'union:member-relation:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21214, 2121, '推广者关系删除', 3, 'union:member-relation:delete', 4);

-- 推广者团队快照
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2122, 2000, '推广者团队快照', 2, '/union/member-team-snapshot', 23);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21221, 2122, '推广者团队快照查询', 3, 'union:member-team-snapshot:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21222, 2122, '推广者团队快照新增', 3, 'union:member-team-snapshot:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21223, 2122, '推广者团队快照修改', 3, 'union:member-team-snapshot:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21224, 2122, '推广者团队快照删除', 3, 'union:member-team-snapshot:delete', 4);

-- 消息通知
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2123, 2000, '消息通知', 2, '/union/notification', 24);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21231, 2123, '消息通知查询', 3, 'union:notification:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21232, 2123, '消息通知新增', 3, 'union:notification:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21233, 2123, '消息通知修改', 3, 'union:notification:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21234, 2123, '消息通知删除', 3, 'union:notification:delete', 4);

-- 订单归因日志
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2124, 2000, '订单归因日志', 2, '/union/order-attribution-log', 25);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21241, 2124, '订单归因日志查询', 3, 'union:order-attribution-log:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21242, 2124, '订单归因日志新增', 3, 'union:order-attribution-log:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21243, 2124, '订单归因日志修改', 3, 'union:order-attribution-log:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21244, 2124, '订单归因日志删除', 3, 'union:order-attribution-log:delete', 4);

-- 联盟订单主
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2125, 2000, '联盟订单主', 2, '/union/order', 26);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21251, 2125, '联盟订单主查询', 3, 'union:order:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21252, 2125, '联盟订单主新增', 3, 'union:order:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21253, 2125, '联盟订单主修改', 3, 'union:order:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21254, 2125, '联盟订单主删除', 3, 'union:order:delete', 4);

-- 平台类目映射
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2126, 2000, '平台类目映射', 2, '/union/platform-category-map', 27);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21261, 2126, '平台类目映射查询', 3, 'union:platform-category-map:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21262, 2126, '平台类目映射新增', 3, 'union:platform-category-map:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21263, 2126, '平台类目映射修改', 3, 'union:platform-category-map:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21264, 2126, '平台类目映射删除', 3, 'union:platform-category-map:delete', 4);

-- 商品分类关联
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2127, 2000, '商品分类关联', 2, '/union/product-category', 28);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21271, 2127, '商品分类关联查询', 3, 'union:product-category:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21272, 2127, '商品分类关联新增', 3, 'union:product-category:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21273, 2127, '商品分类关联修改', 3, 'union:product-category:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21274, 2127, '商品分类关联删除', 3, 'union:product-category:delete', 4);

-- 联盟商品主
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2128, 2000, '联盟商品主', 2, '/union/product', 29);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21281, 2128, '联盟商品主查询', 3, 'union:product:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21282, 2128, '联盟商品主新增', 3, 'union:product:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21283, 2128, '联盟商品主修改', 3, 'union:product:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21284, 2128, '联盟商品主删除', 3, 'union:product:delete', 4);

-- 商品扩展信息
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2129, 2000, '商品扩展信息', 2, '/union/product-extra', 30);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21291, 2129, '商品扩展信息查询', 3, 'union:product-extra:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21292, 2129, '商品扩展信息新增', 3, 'union:product-extra:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21293, 2129, '商品扩展信息修改', 3, 'union:product-extra:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21294, 2129, '商品扩展信息删除', 3, 'union:product-extra:delete', 4);

-- 商品状态变更日志
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2130, 2000, '商品状态变更日志', 2, '/union/product-status-log', 31);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21301, 2130, '商品状态变更日志查询', 3, 'union:product-status-log:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21302, 2130, '商品状态变更日志新增', 3, 'union:product-status-log:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21303, 2130, '商品状态变更日志修改', 3, 'union:product-status-log:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21304, 2130, '商品状态变更日志删除', 3, 'union:product-status-log:delete', 4);

-- 商品同步日志
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2131, 2000, '商品同步日志', 2, '/union/product-sync-log', 32);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21311, 2131, '商品同步日志查询', 3, 'union:product-sync-log:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21312, 2131, '商品同步日志新增', 3, 'union:product-sync-log:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21313, 2131, '商品同步日志修改', 3, 'union:product-sync-log:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21314, 2131, '商品同步日志删除', 3, 'union:product-sync-log:delete', 4);

-- 推广链接
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2132, 2000, '推广链接', 2, '/union/promotion-link', 33);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21321, 2132, '推广链接查询', 3, 'union:promotion-link:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21322, 2132, '推广链接新增', 3, 'union:promotion-link:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21323, 2132, '推广链接修改', 3, 'union:promotion-link:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21324, 2132, '推广链接删除', 3, 'union:promotion-link:delete', 4);

-- 推广文案模板
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2133, 2000, '推广文案模板', 2, '/union/promotion-template', 34);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21331, 2133, '推广文案模板查询', 3, 'union:promotion-template:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21332, 2133, '推广文案模板新增', 3, 'union:promotion-template:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21333, 2133, '推广文案模板修改', 3, 'union:promotion-template:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21334, 2133, '推广文案模板删除', 3, 'union:promotion-template:delete', 4);

-- 风控黑名单
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2134, 2000, '风控黑名单', 2, '/union/risk-blacklist', 35);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21341, 2134, '风控黑名单查询', 3, 'union:risk-blacklist:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21342, 2134, '风控黑名单新增', 3, 'union:risk-blacklist:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21343, 2134, '风控黑名单修改', 3, 'union:risk-blacklist:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21344, 2134, '风控黑名单删除', 3, 'union:risk-blacklist:delete', 4);

-- 风控执行记录
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2135, 2000, '风控执行记录', 2, '/union/risk-execution-log', 36);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21351, 2135, '风控执行记录查询', 3, 'union:risk-execution-log:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21352, 2135, '风控执行记录新增', 3, 'union:risk-execution-log:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21353, 2135, '风控执行记录修改', 3, 'union:risk-execution-log:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21354, 2135, '风控执行记录删除', 3, 'union:risk-execution-log:delete', 4);

-- 风控规则配置
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2136, 2000, '风控规则配置', 2, '/union/risk-rule-config', 37);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21361, 2136, '风控规则配置查询', 3, 'union:risk-rule-config:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21362, 2136, '风控规则配置新增', 3, 'union:risk-rule-config:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21363, 2136, '风控规则配置修改', 3, 'union:risk-rule-config:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21364, 2136, '风控规则配置删除', 3, 'union:risk-rule-config:delete', 4);

-- 结算批次
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2137, 2000, '结算批次', 2, '/union/settle-batch', 38);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21371, 2137, '结算批次查询', 3, 'union:settle-batch:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21372, 2137, '结算批次新增', 3, 'union:settle-batch:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21373, 2137, '结算批次修改', 3, 'union:settle-batch:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21374, 2137, '结算批次删除', 3, 'union:settle-batch:delete', 4);

-- 短链接映射
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2138, 2000, '短链接映射', 2, '/union/short-url-map', 39);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21381, 2138, '短链接映射查询', 3, 'union:short-url-map:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21382, 2138, '短链接映射新增', 3, 'union:short-url-map:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21383, 2138, '短链接映射修改', 3, 'union:short-url-map:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21384, 2138, '短链接映射删除', 3, 'union:short-url-map:delete', 4);

-- 提现申请
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2139, 2000, '提现申请', 2, '/union/withdraw-apply', 40);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21391, 2139, '提现申请查询', 3, 'union:withdraw-apply:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21392, 2139, '提现申请新增', 3, 'union:withdraw-apply:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21393, 2139, '提现申请修改', 3, 'union:withdraw-apply:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21394, 2139, '提现申请删除', 3, 'union:withdraw-apply:delete', 4);

-- 提现审核日志
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2140, 2000, '提现审核日志', 2, '/union/withdraw-audit-log', 41);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21401, 2140, '提现审核日志查询', 3, 'union:withdraw-audit-log:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21402, 2140, '提现审核日志新增', 3, 'union:withdraw-audit-log:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21403, 2140, '提现审核日志修改', 3, 'union:withdraw-audit-log:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21404, 2140, '提现审核日志删除', 3, 'union:withdraw-audit-log:delete', 4);

-- 提现配置
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2141, 2000, '提现配置', 2, '/union/withdraw-config', 42);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21411, 2141, '提现配置查询', 3, 'union:withdraw-config:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21412, 2141, '提现配置新增', 3, 'union:withdraw-config:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21413, 2141, '提现配置修改', 3, 'union:withdraw-config:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21414, 2141, '提现配置删除', 3, 'union:withdraw-config:delete', 4);

-- 提现风控记录
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2142, 2000, '提现风控记录', 2, '/union/withdraw-risk-record', 43);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21421, 2142, '提现风控记录查询', 3, 'union:withdraw-risk-record:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21422, 2142, '提现风控记录新增', 3, 'union:withdraw-risk-record:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21423, 2142, '提现风控记录修改', 3, 'union:withdraw-risk-record:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21424, 2142, '提现风控记录删除', 3, 'union:withdraw-risk-record:delete', 4);

-- 提现白名单
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `sort`) VALUES (2143, 2000, '提现白名单', 2, '/union/withdraw-whitelist', 44);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21431, 2143, '提现白名单查询', 3, 'union:withdraw-whitelist:query', 1);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21432, 2143, '提现白名单新增', 3, 'union:withdraw-whitelist:create', 2);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21433, 2143, '提现白名单修改', 3, 'union:withdraw-whitelist:update', 3);
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `permission`, `sort`) VALUES (21434, 2143, '提现白名单删除', 3, 'union:withdraw-whitelist:delete', 4);

-- 超级管理员角色授权（目录 + 全部菜单与按钮）
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1000, 1, 2000);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1001, 1, 2100);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1002, 1, 21001);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1003, 1, 21002);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1004, 1, 21003);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1005, 1, 21004);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1006, 1, 2101);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1007, 1, 21011);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1008, 1, 21012);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1009, 1, 21013);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1010, 1, 21014);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1011, 1, 2102);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1012, 1, 21021);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1013, 1, 21022);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1014, 1, 21023);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1015, 1, 21024);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1016, 1, 2103);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1017, 1, 21031);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1018, 1, 21032);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1019, 1, 21033);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1020, 1, 21034);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1021, 1, 2104);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1022, 1, 21041);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1023, 1, 21042);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1024, 1, 21043);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1025, 1, 21044);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1026, 1, 2105);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1027, 1, 21051);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1028, 1, 21052);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1029, 1, 21053);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1030, 1, 21054);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1031, 1, 2106);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1032, 1, 21061);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1033, 1, 21062);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1034, 1, 21063);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1035, 1, 21064);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1036, 1, 2107);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1037, 1, 21071);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1038, 1, 21072);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1039, 1, 21073);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1040, 1, 21074);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1041, 1, 2108);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1042, 1, 21081);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1043, 1, 21082);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1044, 1, 21083);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1045, 1, 21084);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1046, 1, 2109);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1047, 1, 21091);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1048, 1, 21092);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1049, 1, 21093);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1050, 1, 21094);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1051, 1, 2110);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1052, 1, 21101);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1053, 1, 21102);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1054, 1, 21103);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1055, 1, 21104);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1056, 1, 2111);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1057, 1, 21111);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1058, 1, 21112);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1059, 1, 21113);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1060, 1, 21114);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1061, 1, 2112);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1062, 1, 21121);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1063, 1, 21122);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1064, 1, 21123);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1065, 1, 21124);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1066, 1, 2113);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1067, 1, 21131);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1068, 1, 21132);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1069, 1, 21133);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1070, 1, 21134);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1071, 1, 2114);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1072, 1, 21141);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1073, 1, 21142);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1074, 1, 21143);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1075, 1, 21144);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1076, 1, 2115);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1077, 1, 21151);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1078, 1, 21152);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1079, 1, 21153);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1080, 1, 21154);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1081, 1, 2116);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1082, 1, 21161);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1083, 1, 21162);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1084, 1, 21163);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1085, 1, 21164);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1086, 1, 2117);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1087, 1, 21171);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1088, 1, 21172);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1089, 1, 21173);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1090, 1, 21174);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1091, 1, 2118);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1092, 1, 21181);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1093, 1, 21182);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1094, 1, 21183);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1095, 1, 21184);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1096, 1, 2119);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1097, 1, 21191);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1098, 1, 21192);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1099, 1, 21193);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1100, 1, 21194);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1101, 1, 2120);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1102, 1, 21201);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1103, 1, 21202);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1104, 1, 21203);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1105, 1, 21204);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1106, 1, 2121);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1107, 1, 21211);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1108, 1, 21212);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1109, 1, 21213);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1110, 1, 21214);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1111, 1, 2122);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1112, 1, 21221);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1113, 1, 21222);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1114, 1, 21223);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1115, 1, 21224);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1116, 1, 2123);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1117, 1, 21231);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1118, 1, 21232);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1119, 1, 21233);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1120, 1, 21234);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1121, 1, 2124);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1122, 1, 21241);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1123, 1, 21242);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1124, 1, 21243);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1125, 1, 21244);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1126, 1, 2125);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1127, 1, 21251);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1128, 1, 21252);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1129, 1, 21253);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1130, 1, 21254);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1131, 1, 2126);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1132, 1, 21261);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1133, 1, 21262);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1134, 1, 21263);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1135, 1, 21264);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1136, 1, 2127);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1137, 1, 21271);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1138, 1, 21272);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1139, 1, 21273);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1140, 1, 21274);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1141, 1, 2128);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1142, 1, 21281);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1143, 1, 21282);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1144, 1, 21283);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1145, 1, 21284);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1146, 1, 2129);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1147, 1, 21291);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1148, 1, 21292);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1149, 1, 21293);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1150, 1, 21294);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1151, 1, 2130);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1152, 1, 21301);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1153, 1, 21302);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1154, 1, 21303);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1155, 1, 21304);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1156, 1, 2131);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1157, 1, 21311);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1158, 1, 21312);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1159, 1, 21313);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1160, 1, 21314);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1161, 1, 2132);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1162, 1, 21321);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1163, 1, 21322);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1164, 1, 21323);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1165, 1, 21324);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1166, 1, 2133);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1167, 1, 21331);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1168, 1, 21332);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1169, 1, 21333);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1170, 1, 21334);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1171, 1, 2134);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1172, 1, 21341);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1173, 1, 21342);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1174, 1, 21343);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1175, 1, 21344);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1176, 1, 2135);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1177, 1, 21351);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1178, 1, 21352);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1179, 1, 21353);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1180, 1, 21354);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1181, 1, 2136);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1182, 1, 21361);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1183, 1, 21362);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1184, 1, 21363);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1185, 1, 21364);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1186, 1, 2137);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1187, 1, 21371);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1188, 1, 21372);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1189, 1, 21373);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1190, 1, 21374);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1191, 1, 2138);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1192, 1, 21381);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1193, 1, 21382);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1194, 1, 21383);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1195, 1, 21384);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1196, 1, 2139);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1197, 1, 21391);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1198, 1, 21392);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1199, 1, 21393);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1200, 1, 21394);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1201, 1, 2140);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1202, 1, 21401);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1203, 1, 21402);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1204, 1, 21403);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1205, 1, 21404);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1206, 1, 2141);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1207, 1, 21411);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1208, 1, 21412);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1209, 1, 21413);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1210, 1, 21414);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1211, 1, 2142);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1212, 1, 21421);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1213, 1, 21422);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1214, 1, 21423);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1215, 1, 21424);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1216, 1, 2143);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1217, 1, 21431);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1218, 1, 21432);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1219, 1, 21433);
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES (1220, 1, 21434);
