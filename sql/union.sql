-- ------------------------------------------------------------
-- 会员用户表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `member_user`;
CREATE TABLE `member_user` (
    `id`           BIGINT       NOT NULL COMMENT '主键ID（雪花算法）',
    `mobile`       VARCHAR(20)  NOT NULL DEFAULT '' COMMENT '手机号',
    `password`     VARCHAR(100) NOT NULL DEFAULT '' COMMENT '登录密码（BCrypt，未设置为空）',
    `nickname`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '昵称',
    `avatar`       VARCHAR(512) NOT NULL DEFAULT '' COMMENT '头像地址',
    `gender`       TINYINT      NOT NULL DEFAULT 0 COMMENT '性别：0-未知 1-男 2-女',
    `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `register_ip`  VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '注册IP',
    `register_type` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '注册来源：WECHAT_MINI-小程序 PASSWORD-账号密码',
    `login_ip`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '最后登录IP',
    `login_date`   DATETIME         NULL COMMENT '最后登录时间',
    `create_by`    BIGINT           NULL COMMENT '创建人ID',
    `create_time`  DATETIME         NULL COMMENT '创建时间',
    `update_by`    BIGINT           NULL COMMENT '更新人ID',
    `update_time`  DATETIME         NULL COMMENT '更新时间',
    `is_delete`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_mobile` (`mobile`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '会员用户表';

-- ------------------------------------------------------------
-- 会员第三方授权表（微信小程序、微信开放平台、支付宝等）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `member_social_user`;
CREATE TABLE `member_social_user` (
    `id`           BIGINT       NOT NULL COMMENT '主键ID（雪花算法）',
    `user_id`      BIGINT       NOT NULL COMMENT '会员用户ID',
    `social_type`  VARCHAR(32)  NOT NULL COMMENT '第三方类型：WECHAT_MINI-微信小程序 WECHAT_MP-微信公众号 WECHAT_OPEN-开放平台 ALIPAY APPLE ...',
    `open_id`      VARCHAR(128) NOT NULL COMMENT '第三方用户唯一标识（openid）',
    `union_id`     VARCHAR(128) NOT NULL DEFAULT '' COMMENT '第三方 UnionId（同主体多应用打通）',
    `session_key`  VARCHAR(128) NOT NULL DEFAULT '' COMMENT '微信 session_key（小程序）',
    `nickname`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '第三方昵称',
    `avatar`       VARCHAR(512) NOT NULL DEFAULT '' COMMENT '第三方头像',
    `raw_info`     VARCHAR(2048) NOT NULL DEFAULT '' COMMENT '第三方原始信息（JSON）',
    `create_by`    BIGINT           NULL COMMENT '创建人ID',
    `create_time`  DATETIME         NULL COMMENT '创建时间',
    `update_by`    BIGINT           NULL COMMENT '更新人ID',
    `update_time`  DATETIME         NULL COMMENT '更新时间',
    `is_delete`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_openid` (`social_type`, `open_id`, `is_delete`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_union_id` (`union_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '会员第三方授权表';

CREATE TABLE `union_product` (
     `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
     `platform` tinyint(4) NOT NULL COMMENT '平台：1-淘宝 2-京东 3-拼多多 4-抖音',
     `outer_product_id` varchar(64) NOT NULL COMMENT '第三方平台原始商品ID（京东为字符串）',
     `title` varchar(512) NOT NULL COMMENT '商品标题',
     `sub_title` varchar(256) DEFAULT NULL COMMENT '副标题/卖点',
     `image_url` varchar(1024) NOT NULL COMMENT '主图URL',
     `image_list` text COMMENT '商品轮播图列表，JSON数组',
     `market_price` decimal(12,2) DEFAULT NULL COMMENT '市场价（划掉的价格）',
     `sale_price` decimal(12,2) NOT NULL COMMENT '当前售价',
     `commission_rate` decimal(8,2) NOT NULL COMMENT '佣金比率（平台原始值，淘宝为千分比需转换）',
     `commission_amount` decimal(12,2) DEFAULT NULL COMMENT '预估佣金金额（预计算字段）',
     `shop_name` varchar(128) DEFAULT NULL COMMENT '店铺名称',
     `shop_id` varchar(64) DEFAULT NULL COMMENT '店铺ID',
     `category_id` varchar(64) DEFAULT NULL COMMENT '类目ID',
     `category_name` varchar(128) DEFAULT NULL COMMENT '类目名称',
     `sales_volume` int(11) DEFAULT '0' COMMENT '月销量',
     `good_comments` int(11) DEFAULT '0' COMMENT '好评数',
     `coupon_id` varchar(64) DEFAULT NULL COMMENT '优惠券ID',
     `coupon_amount` decimal(12,2) DEFAULT NULL COMMENT '优惠券面额',
     `coupon_start_time` datetime DEFAULT NULL COMMENT '优惠券开始时间',
     `coupon_end_time` datetime DEFAULT NULL COMMENT '优惠券结束时间',
     `is_recommend` tinyint(1) DEFAULT '0' COMMENT '是否推荐：0-否 1-是',
     `status` tinyint(4) DEFAULT '1' COMMENT '状态：1-上架 2-下架 3-审核中',
     `sync_status` tinyint(4) DEFAULT '0' COMMENT '同步状态：0-未同步 1-已同步 2-同步失败',
     `last_sync_time` datetime DEFAULT NULL COMMENT '最后同步时间',
     `next_sync_time` datetime DEFAULT NULL COMMENT '下次同步时间（用于增量同步）',
     `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
     `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
     PRIMARY KEY (`id`),
     UNIQUE KEY `uk_platform_outer` (`platform`, `outer_product_id`),
     KEY `idx_status_sync` (`status`, `sync_status`),
     KEY `idx_commission_rate` (`commission_rate`),
     KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联盟商品主表';


CREATE TABLE `union_product_extra` (
   `id` bigint(20) NOT NULL AUTO_INCREMENT,
   `product_id` bigint(20) NOT NULL COMMENT '关联union_product.id',
   `detail_url` varchar(1024) DEFAULT NULL COMMENT '商品详情页URL',
   `desc` longtext COMMENT '商品描述（富文本）',
   `brand_name` varchar(128) DEFAULT NULL COMMENT '品牌名称',
   `brand_id` varchar(64) DEFAULT NULL COMMENT '品牌ID',
   `province` varchar(32) DEFAULT NULL COMMENT '发货地省份',
   `shipping_fee` decimal(10,2) DEFAULT '0.00' COMMENT '运费',
   `after_sale_service` varchar(256) DEFAULT NULL COMMENT '售后服务',
   `extra_json` json DEFAULT NULL COMMENT '其他扩展字段（JSON）',
   `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
   `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
   PRIMARY KEY (`id`),
   UNIQUE KEY `uk_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品扩展信息表';


CREATE TABLE `union_product_category` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `product_id` bigint(20) NOT NULL,
  `category_id` bigint(20) NOT NULL COMMENT '分类ID',
  `sort_order` int(11) DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_category` (`product_id`, `category_id`),
  KEY `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类关联表';


CREATE TABLE `union_category` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `parent_id` bigint(20) DEFAULT '0' COMMENT '父级ID（0表示顶级）',
  `name` varchar(64) NOT NULL COMMENT '分类名称',
  `icon` varchar(256) DEFAULT NULL COMMENT '分类图标URL',
  `sort_order` int(11) DEFAULT '0',
  `status` tinyint(4) DEFAULT '1' COMMENT '1-启用 0-禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

CREATE TABLE `union_product_sync_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `platform` tinyint(4) NOT NULL COMMENT '平台',
  `sync_type` tinyint(4) NOT NULL COMMENT '同步类型：1-全量 2-增量 3-手动',
  `total_count` int(11) DEFAULT '0' COMMENT '同步总数',
  `success_count` int(11) DEFAULT '0' COMMENT '成功数',
  `fail_count` int(11) DEFAULT '0' COMMENT '失败数',
  `error_msg` text COMMENT '错误信息',
  `start_time` datetime NOT NULL,
  `end_time` datetime DEFAULT NULL,
  `status` tinyint(4) DEFAULT '0' COMMENT '0-进行中 1-成功 2-失败',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_platform_status` (`platform`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品同步日志表';


CREATE TABLE `union_order` (
   `id` bigint(20) NOT NULL AUTO_INCREMENT,
   `platform` tinyint(4) NOT NULL COMMENT '平台：1-淘宝 2-京东 3-拼多多',
   `outer_order_id` varchar(64) NOT NULL COMMENT '第三方平台订单号',
   `parent_order_id` varchar(64) DEFAULT NULL COMMENT '父订单号（合并支付时）',
   `member_id` bigint(20) DEFAULT NULL COMMENT '推广者ID（归因后填充）',
   `promotion_link_id` bigint(20) DEFAULT NULL COMMENT '关联推广链接记录ID',

    -- 商品信息（冗余存储，防止商品信息变更影响订单）
   `product_id` bigint(20) DEFAULT NULL COMMENT '关联union_product.id',
   `product_title` varchar(512) DEFAULT NULL COMMENT '商品标题（冗余）',
   `product_image` varchar(1024) DEFAULT NULL COMMENT '商品主图（冗余）',
   `outer_product_id` varchar(64) DEFAULT NULL COMMENT '第三方商品ID（冗余）',
   `shop_name` varchar(128) DEFAULT NULL COMMENT '店铺名称（冗余）',

    -- 价格与佣金
   `order_amount` decimal(12,2) NOT NULL COMMENT '订单金额（实际支付）',
   `commission_rate` decimal(8,2) DEFAULT NULL COMMENT '该笔订单的佣金比率（快照）',
   `commission_amount` decimal(12,2) DEFAULT NULL COMMENT '预估佣金金额',
   `actual_commission` decimal(12,2) DEFAULT NULL COMMENT '最终结算佣金（平台结算后确定）',

    -- 优惠信息
   `coupon_amount` decimal(12,2) DEFAULT '0.00' COMMENT '优惠券抵扣金额',

    -- 订单状态（各平台状态映射到统一状态机）
   `order_status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '订单状态：1-待支付 2-已支付 3-已收货 4-已结算 5-无效/退款 6-已关闭',
   `platform_order_status` varchar(32) DEFAULT NULL COMMENT '平台原始订单状态码',

    -- 结算状态
   `settle_status` tinyint(4) DEFAULT '0' COMMENT '结算状态：0-未结算 1-已结算 2-结算失败',
   `settle_time` datetime DEFAULT NULL COMMENT '实际结算时间',

    -- 时间字段
   `order_time` datetime NOT NULL COMMENT '下单时间（第三方）',
   `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
   `confirm_time` datetime DEFAULT NULL COMMENT '确认收货时间',
   `sync_time` datetime DEFAULT NULL COMMENT '同步到本系统的时间',

    -- 扩展字段
   `extra_json` json DEFAULT NULL COMMENT '平台扩展数据（JSON）',

   `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
   `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
   PRIMARY KEY (`id`),
   UNIQUE KEY `uk_platform_outer_order` (`platform`, `outer_order_id`),
   KEY `idx_member_id` (`member_id`),
   KEY `idx_promotion_link` (`promotion_link_id`),
   KEY `idx_order_time` (`order_time`),
   KEY `idx_order_status` (`order_status`),
   KEY `idx_settle_status` (`settle_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联盟订单主表';


CREATE TABLE `union_commission_record` (
   `id` bigint(20) NOT NULL AUTO_INCREMENT,
   `order_id` bigint(20) NOT NULL COMMENT '关联union_order.id',
   `member_id` bigint(20) NOT NULL COMMENT '推广者ID',
   `product_id` bigint(20) DEFAULT NULL COMMENT '商品ID',

    -- 金额信息
   `order_amount` decimal(12,2) NOT NULL COMMENT '订单金额',
   `commission_rate` decimal(8,2) NOT NULL COMMENT '佣金比率',
   `commission_amount` decimal(12,2) NOT NULL COMMENT '佣金金额（含税）',
   `tax_amount` decimal(12,2) DEFAULT '0.00' COMMENT '税金（如适用）',
   `actual_amount` decimal(12,2) NOT NULL COMMENT '实际入账金额（扣除税费后）',

    -- 状态机
   `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态：1-待计算 2-待入账 3-已入账 4-入账失败 5-已退款 6-已取消',

    -- 结算关联
   `settle_batch_no` varchar(64) DEFAULT NULL COMMENT '结算批次号',
   `settle_time` datetime DEFAULT NULL COMMENT '入账时间',

    -- 业务流水
   `transaction_id` varchar(64) DEFAULT NULL COMMENT '关联资金流水ID',

    -- 扩展
   `remark` varchar(256) DEFAULT NULL COMMENT '备注',
   `retry_count` int(11) DEFAULT '0' COMMENT '重试次数',

   `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
   `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
   PRIMARY KEY (`id`),
   UNIQUE KEY `uk_order_id` (`order_id`),
   KEY `idx_member_id` (`member_id`),
   KEY `idx_status` (`status`),
   KEY `idx_settle_batch` (`settle_batch_no`),
   KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='佣金记录表';


CREATE TABLE `union_settle_batch` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `batch_no` varchar(64) NOT NULL COMMENT '批次号（业务唯一）',
  `total_amount` decimal(12,2) NOT NULL COMMENT '批次总金额',
  `record_count` int(11) NOT NULL COMMENT '包含记录数',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态：1-待审核 2-审核通过 3-已结算 4-已驳回',
  `operator_id` bigint(20) DEFAULT NULL COMMENT '操作员ID',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `settle_time` datetime DEFAULT NULL COMMENT '结算完成时间',
  `remark` varchar(256) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_no` (`batch_no`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算批次表';


CREATE TABLE `union_order_attribution_log` (
   `id` bigint(20) NOT NULL AUTO_INCREMENT,
   `order_id` bigint(20) NOT NULL COMMENT '订单ID',
   `platform` tinyint(4) NOT NULL,
   `outer_order_id` varchar(64) NOT NULL,
   `attributed_member_id` bigint(20) DEFAULT NULL COMMENT '归因到的推广者ID',
   `attribution_method` varchar(32) DEFAULT NULL COMMENT '归因方式：cookie/链接/设备ID',
   `confidence_score` decimal(5,2) DEFAULT NULL COMMENT '置信度分数',
   `process_status` tinyint(4) DEFAULT '0' COMMENT '0-待处理 1-成功 2-失败',
   `error_msg` text COMMENT '失败原因',
   `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
   PRIMARY KEY (`id`),
   KEY `idx_order_id` (`order_id`),
   KEY `idx_outer_order` (`platform`, `outer_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单归因日志表';


CREATE TABLE `union_member_relation` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `member_id` bigint(20) NOT NULL COMMENT '推广者ID（本人）',
    `parent_id` bigint(20) DEFAULT NULL COMMENT '上级推广者ID（直接邀请人）',
    `root_id` bigint(20) DEFAULT NULL COMMENT '顶级推广者ID（根节点，用于快速归属）',

    -- 层级路径（核心字段，用于多级查询和佣金计算）
    `path` varchar(1024) DEFAULT NULL COMMENT '层级路径：例如 /1/23/45/ ，从根节点到本人的ID路径',
    `level` int(11) DEFAULT '1' COMMENT '当前层级：1表示顶级，2表示二级，以此类推',
    `max_depth` int(11) DEFAULT '1' COMMENT '该节点以下的最大深度（子树深度，便于缓存）',

    -- 关系状态
    `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态：1-正常 2-已解除 3-已冻结',

    -- 时间字段
    `bind_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间（成为下级的时间）',
    `unbind_time` datetime DEFAULT NULL COMMENT '解绑时间',

    -- 统计字段（可定时更新，避免实时计算）
    `total_children` int(11) DEFAULT '0' COMMENT '直属下级数量',
    `total_team` int(11) DEFAULT '0' COMMENT '整个团队人数（所有层级）',

    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_member_id` (`member_id`), -- 一个推广者只能有一条关系记录
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_root_id` (`root_id`),
    KEY `idx_path` (`path`(255)), -- 前缀索引，用于 `LIKE '/1/23/%'` 查询
    KEY `idx_level` (`level`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推广者关系表';


CREATE TABLE `union_member_team_snapshot` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `member_id` bigint(20) NOT NULL COMMENT '推广者ID',

    -- 团队统计（按层级）
    `level_1_count` int(11) DEFAULT '0' COMMENT '一级下级数',
    `level_2_count` int(11) DEFAULT '0' COMMENT '二级下级数',
    `level_3_count` int(11) DEFAULT '0' COMMENT '三级下级数',
    `total_team_count` int(11) DEFAULT '0' COMMENT '团队总人数',

    -- 团队佣金贡献
    `team_total_commission` decimal(12,2) DEFAULT '0.00' COMMENT '团队累计贡献佣金',
    `team_month_commission` decimal(12,2) DEFAULT '0.00' COMMENT '本月团队贡献佣金',

    -- 统计时间
    `snapshot_date` date NOT NULL COMMENT '快照日期',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_member_date` (`member_id`, `snapshot_date`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_snapshot_date` (`snapshot_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推广者团队快照表';


CREATE TABLE `union_level_config` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `level` int(11) NOT NULL COMMENT '层级（1/2/3/...）',
    `level_name` varchar(64) NOT NULL COMMENT '层级名称（如：合伙人/总监/经理）',
    `commission_ratio` decimal(5,4) NOT NULL COMMENT '该层级的佣金系数（如0.1表示10%）',
    `min_team_size` int(11) DEFAULT '0' COMMENT '晋升该层级所需最小团队人数',
    `min_team_commission` decimal(12,2) DEFAULT '0.00' COMMENT '晋升该层级所需最小团队佣金',
    `is_active` tinyint(1) DEFAULT '1' COMMENT '是否启用',
    `sort_order` int(11) DEFAULT '0' COMMENT '排序',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='层级佣金配置表';


CREATE TABLE `union_commission_rule` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `rule_name` varchar(128) NOT NULL COMMENT '规则名称',
    `rule_type` tinyint(4) NOT NULL COMMENT '规则类型：1-全局默认 2-按商品类目 3-按推广者等级 4-按商品平台',

    -- 平台维度
    `platform` tinyint(4) DEFAULT NULL COMMENT '平台：1-淘宝 2-京东 3-拼多多（NULL表示全部）',
    `category_id` bigint(20) DEFAULT NULL COMMENT '类目ID（NULL表示全部）',
    `member_level` int(11) DEFAULT NULL COMMENT '推广者等级（NULL表示全部）',

    -- 分配比例（核心字段）
    `platform_service_rate` decimal(5,4) DEFAULT '0.0000' COMMENT '平台服务费率（如淘宝10%）',
    `system_cost_rate` decimal(5,4) NOT NULL COMMENT '系统运营成本率（如15%）',
    `distributable_rate` decimal(5,4) NOT NULL COMMENT '可分配佣金率 = 1 - platform_service_rate - system_cost_rate',

    -- 层级分配（直接佣金 + 层级奖励）
    `direct_commission_rate` decimal(5,4) NOT NULL COMMENT '推广者直接佣金率（占可分配池）',
    `level_1_rate` decimal(5,4) DEFAULT '0.0000' COMMENT '一级上级奖励率（占可分配池）',
    `level_2_rate` decimal(5,4) DEFAULT '0.0000' COMMENT '二级上级奖励率（占可分配池）',
    `level_3_rate` decimal(5,4) DEFAULT '0.0000' COMMENT '三级上级奖励率（占可分配池）',
    `level_4_rate` decimal(5,4) DEFAULT '0.0000' COMMENT '四级上级奖励率',
    `level_5_rate` decimal(5,4) DEFAULT '0.0000' COMMENT '五级上级奖励率',

    -- 优先级
    `priority` int(11) DEFAULT '0' COMMENT '优先级（数字越小越优先匹配）',

    -- 状态
    `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '1-启用 0-禁用',
    `start_time` datetime DEFAULT NULL COMMENT '生效开始时间',
    `end_time` datetime DEFAULT NULL COMMENT '生效结束时间',

    `remark` varchar(256) DEFAULT NULL,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    KEY `idx_platform_category` (`platform`, `category_id`),
    KEY `idx_member_level` (`member_level`),
    KEY `idx_priority` (`priority`),
    KEY `idx_status_time` (`status`, `start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='佣金分配规则配置表';


CREATE TABLE `union_member_level` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `level_code` varchar(32) NOT NULL COMMENT '等级编码：L1/L2/L3',
    `level_name` varchar(64) NOT NULL COMMENT '等级名称：见习/正式/黄金/钻石/合伙人',
    `min_team_size` int(11) DEFAULT '0' COMMENT '晋升所需最小团队人数',
    `min_team_commission` decimal(12,2) DEFAULT '0.00' COMMENT '晋升所需最小团队佣金',
    `commission_multiplier` decimal(5,4) DEFAULT '1.0000' COMMENT '佣金系数（等级越高，分配比例越高）',
    `status` tinyint(4) DEFAULT '1',
    `sort_order` int(11) DEFAULT '0',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_level_code` (`level_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推广者等级配置表';

CREATE TABLE `union_commission_distribution` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `order_id` bigint(20) NOT NULL COMMENT '订单ID',
    `commission_record_id` bigint(20) NOT NULL COMMENT '佣金记录ID',

    -- 分配明细
    `receiver_member_id` bigint(20) NOT NULL COMMENT '接收佣金的推广者ID',
    `receiver_level` int(11) NOT NULL COMMENT '接收人的层级（1=本人，2=上级，...）',
    `amount` decimal(12,2) NOT NULL COMMENT '实际分配金额',
    `rate` decimal(5,4) NOT NULL COMMENT '分配比例（占可分配池）',

    -- 分配类型
    `distribution_type` tinyint(4) NOT NULL COMMENT '类型：1-直接佣金 2-层级奖励 3-平台返利',

    -- 状态
    `status` tinyint(4) DEFAULT '1' COMMENT '1-待入账 2-已入账 3-已失效',

    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_member_id` (`receiver_member_id`),
    KEY `idx_record_id` (`commission_record_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='佣金分配明细表';

-- 推广者账户表
CREATE TABLE `union_member_account` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `member_id` bigint(20) NOT NULL COMMENT '推广者ID',
    `total_commission` decimal(12,2) DEFAULT '0.00' COMMENT '累计佣金总额',
    `available_balance` decimal(12,2) DEFAULT '0.00' COMMENT '可提现余额',
    `frozen_balance` decimal(12,2) DEFAULT '0.00' COMMENT '冻结余额（提现中）',
    `total_withdrawn` decimal(12,2) DEFAULT '0.00' COMMENT '累计已提现',
    `pending_settle` decimal(12,2) DEFAULT '0.00' COMMENT '待结算佣金（未到结算周期）',
    `version` int(11) DEFAULT '0' COMMENT '乐观锁版本号',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_member_id` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推广者账户表';

-- 资金流水表（记录所有余额变动）
CREATE TABLE `union_account_transaction` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `member_id` bigint(20) NOT NULL COMMENT '推广者ID',
    `transaction_no` varchar(64) NOT NULL COMMENT '流水号（唯一）',
    `transaction_type` tinyint(4) NOT NULL COMMENT '类型：1-佣金入账 2-提现扣减 3-系统调整 4-退款回滚 5-税费扣除',
    `amount` decimal(12,2) NOT NULL COMMENT '变动金额（正数增加，负数减少）',
    `balance_before` decimal(12,2) NOT NULL COMMENT '变动前余额',
    `balance_after` decimal(12,2) NOT NULL COMMENT '变动后余额',
    `source_id` varchar(64) DEFAULT NULL COMMENT '来源ID（如订单ID、提现申请ID）',
    `remark` varchar(256) DEFAULT NULL COMMENT '备注',
    `status` tinyint(4) DEFAULT '1' COMMENT '1-成功 2-失败 3-处理中',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_transaction_no` (`transaction_no`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户资金流水表';

-- 推广者账户表
CREATE TABLE `union_member_account` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `member_id` bigint(20) NOT NULL COMMENT '推广者ID',
    `total_commission` decimal(12,2) DEFAULT '0.00' COMMENT '累计佣金总额',
    `available_balance` decimal(12,2) DEFAULT '0.00' COMMENT '可提现余额',
    `frozen_balance` decimal(12,2) DEFAULT '0.00' COMMENT '冻结余额（提现中）',
    `total_withdrawn` decimal(12,2) DEFAULT '0.00' COMMENT '累计已提现',
    `pending_settle` decimal(12,2) DEFAULT '0.00' COMMENT '待结算佣金（未到结算周期）',
    `version` int(11) DEFAULT '0' COMMENT '乐观锁版本号',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_member_id` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推广者账户表';

-- 资金流水表（记录所有余额变动）
CREATE TABLE `union_account_transaction` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `member_id` bigint(20) NOT NULL COMMENT '推广者ID',
    `transaction_no` varchar(64) NOT NULL COMMENT '流水号（唯一）',
    `transaction_type` tinyint(4) NOT NULL COMMENT '类型：1-佣金入账 2-提现扣减 3-系统调整 4-退款回滚 5-税费扣除',
    `amount` decimal(12,2) NOT NULL COMMENT '变动金额（正数增加，负数减少）',
    `balance_before` decimal(12,2) NOT NULL COMMENT '变动前余额',
    `balance_after` decimal(12,2) NOT NULL COMMENT '变动后余额',
    `source_id` varchar(64) DEFAULT NULL COMMENT '来源ID（如订单ID、提现申请ID）',
    `remark` varchar(256) DEFAULT NULL COMMENT '备注',
    `status` tinyint(4) DEFAULT '1' COMMENT '1-成功 2-失败 3-处理中',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_transaction_no` (`transaction_no`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户资金流水表';

-- 推广链接表
CREATE TABLE `union_promotion_link` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `member_id` bigint(20) NOT NULL COMMENT '推广者ID',
    `platform` tinyint(4) NOT NULL COMMENT '平台',
    `product_id` bigint(20) DEFAULT NULL COMMENT '商品ID（单品推广）',
    `shop_id` varchar(64) DEFAULT NULL COMMENT '店铺ID（店铺推广）',
    `link_type` tinyint(4) NOT NULL COMMENT '链接类型：1-单品 2-店铺 3-活动 4-通用',
    `promotion_url` varchar(2048) DEFAULT NULL COMMENT '推广长链接',
    `short_url` varchar(128) DEFAULT NULL COMMENT '短链接',
    `promotion_code` varchar(128) DEFAULT NULL COMMENT '淘口令/口令码',
    `expire_time` datetime DEFAULT NULL COMMENT '有效期（NULL表示永久）',
    `click_count` int(11) DEFAULT '0' COMMENT '点击量',
    `order_count` int(11) DEFAULT '0' COMMENT '转化订单数',
    `status` tinyint(4) DEFAULT '1' COMMENT '1-正常 2-已失效',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_platform` (`platform`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推广链接表';

-- ------------------------------------------------------------
-- 转链请求日志表（记录每次转链请求的完整信息）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `union_link_request_log`;
CREATE TABLE `union_link_request_log` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `member_id` bigint(20) NOT NULL COMMENT '推广者ID',
    `platform` tinyint(4) NOT NULL COMMENT '平台',
    `source_type` tinyint(4) NOT NULL COMMENT '来源类型：1-商品链接 2-商品ID 3-淘口令 4-搜索关键词',
    `source_input` varchar(2048) NOT NULL COMMENT '用户输入的原始内容（脱敏后）',
    `outer_product_id` varchar(64) DEFAULT NULL COMMENT '解析出的第三方商品ID',
    `product_id` bigint(20) DEFAULT NULL COMMENT '关联union_product.id（可能为NULL，首次转链时商品未入库）',
    `link_type` tinyint(4) NOT NULL COMMENT '链接类型：1-单品 2-店铺 3-活动 4-通用',
    `request_ip` varchar(64) DEFAULT NULL COMMENT '请求IP',
    `user_agent` varchar(256) DEFAULT NULL COMMENT '用户代理',

    -- 转链结果
    `result_status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '转链结果：1-成功 2-失败 3-部分成功',
    `promotion_link_id` bigint(20) DEFAULT NULL COMMENT '生成的推广链接ID（成功时）',
    `promotion_url` varchar(2048) DEFAULT NULL COMMENT '生成的推广链接',
    `promotion_code` varchar(128) DEFAULT NULL COMMENT '淘口令/口令码',
    `short_url` varchar(128) DEFAULT NULL COMMENT '短链接',
    `error_code` varchar(32) DEFAULT NULL COMMENT '错误码',
    `error_msg` varchar(512) DEFAULT NULL COMMENT '错误信息',

    -- 性能监控
    `api_call_duration` int(11) DEFAULT '0' COMMENT '调用第三方API耗时（毫秒）',
    `total_duration` int(11) DEFAULT '0' COMMENT '总耗时（毫秒）',

    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_platform` (`platform`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_result_status` (`result_status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='转链请求日志表';


-- ------------------------------------------------------------
-- 短链接映射表（短链接 → 原始推广链接的映射）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `union_short_url_map`;
CREATE TABLE `union_short_url_map` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `short_code` varchar(32) NOT NULL COMMENT '短链码（如：abc123）',
    `promotion_link_id` bigint(20) NOT NULL COMMENT '关联推广链接ID',
    `original_url` varchar(2048) NOT NULL COMMENT '原始推广链接',
    `target_url` varchar(2048) DEFAULT NULL COMMENT '最终跳转URL（可做A/B测试或渠道跟踪）',

    -- 有效期
    `expire_time` datetime DEFAULT NULL COMMENT '过期时间（NULL表示永久）',

    -- 点击统计（实时更新，可配合Redis）
    `total_clicks` int(11) DEFAULT '0' COMMENT '总点击量',
    `unique_clicks` int(11) DEFAULT '0' COMMENT '独立访客点击量（去重）',
    `last_click_time` datetime DEFAULT NULL COMMENT '最后点击时间',

    `status` tinyint(4) DEFAULT '1' COMMENT '状态：1-正常 2-已禁用 3-已过期',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_short_code` (`short_code`),
    KEY `idx_promotion_link_id` (`promotion_link_id`),
    KEY `idx_status_expire` (`status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短链接映射表';

-- ------------------------------------------------------------
-- 推广链接点击明细表（每次点击记录）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `union_click_tracking`;
CREATE TABLE `union_click_tracking` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `short_code` varchar(32) NOT NULL COMMENT '短链码',
    `promotion_link_id` bigint(20) NOT NULL COMMENT '推广链接ID',
    `member_id` bigint(20) NOT NULL COMMENT '推广者ID',
    `platform` tinyint(4) NOT NULL COMMENT '平台',
    `product_id` bigint(20) DEFAULT NULL COMMENT '商品ID',

    -- 用户信息
    `user_id` bigint(20) DEFAULT NULL COMMENT '登录用户ID（如果已登录）',
    `visitor_id` varchar(64) DEFAULT NULL COMMENT '访客ID（设备指纹/Cookie）',
    `click_ip` varchar(64) DEFAULT NULL COMMENT '点击IP',
    `user_agent` varchar(512) DEFAULT NULL COMMENT '用户代理',
    `referer` varchar(1024) DEFAULT NULL COMMENT '来源页面',

    -- 地理位置
    `province` varchar(32) DEFAULT NULL COMMENT '省份',
    `city` varchar(32) DEFAULT NULL COMMENT '城市',

    -- 是否转化（后续更新）
    `is_converted` tinyint(1) DEFAULT '0' COMMENT '是否产生订单：0-否 1-是',
    `order_id` bigint(20) DEFAULT NULL COMMENT '关联订单ID（归因后填充）',
    `order_amount` decimal(12,2) DEFAULT NULL COMMENT '订单金额',

    `click_time` datetime NOT NULL COMMENT '点击时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    KEY `idx_short_code` (`short_code`),
    KEY `idx_promotion_link_id` (`promotion_link_id`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_visitor_id` (`visitor_id`),
    KEY `idx_click_time` (`click_time`),
    KEY `idx_is_converted` (`is_converted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推广链接点击明细表';


-- ------------------------------------------------------------
-- 推广文案模板表（按平台/商品类目配置不同文案模板）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `union_promotion_template`;
CREATE TABLE `union_promotion_template` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `template_name` varchar(128) NOT NULL COMMENT '模板名称',
    `platform` tinyint(4) DEFAULT NULL COMMENT '平台（NULL表示通用）',
    `category_id` bigint(20) DEFAULT NULL COMMENT '类目ID（NULL表示通用）',
    `template_type` tinyint(4) NOT NULL COMMENT '模板类型：1-文案 2-海报 3-视频',

    -- 模板内容
    `title_template` varchar(256) DEFAULT NULL COMMENT '标题模板（支持变量：{title}、{price}、{commission}）',
    `desc_template` varchar(512) DEFAULT NULL COMMENT '描述模板',
    `image_url` varchar(1024) DEFAULT NULL COMMENT '海报底图URL',
    `image_config` json DEFAULT NULL COMMENT '海报配置（文字位置、颜色、字体大小等）',

    `priority` int(11) DEFAULT '0' COMMENT '优先级',
    `status` tinyint(4) DEFAULT '1' COMMENT '1-启用 0-禁用',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    KEY `idx_platform_category` (`platform`, `category_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推广文案模板表';


-- ------------------------------------------------------------
-- 平台API调用记录表（监控各平台API调用情况）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `union_api_call_log`;
CREATE TABLE `union_api_call_log` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `platform` tinyint(4) NOT NULL COMMENT '平台',
    `api_name` varchar(64) NOT NULL COMMENT 'API名称（如：taobao.tbk.item.get）',
    `request_params` json DEFAULT NULL COMMENT '请求参数（脱敏）',
    `response_code` varchar(16) DEFAULT NULL COMMENT '响应码',
    `response_msg` varchar(256) DEFAULT NULL COMMENT '响应消息',
    `is_success` tinyint(1) DEFAULT '1' COMMENT '是否成功',
    `duration` int(11) DEFAULT '0' COMMENT '耗时（毫秒）',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    KEY `idx_platform_api` (`platform`, `api_name`),
    KEY `idx_is_success` (`is_success`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台API调用日志表';


-- ------------------------------------------------------------
-- 转链配置表（按推广者等级、商品类目配置不同的转链策略）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `union_link_config`;
CREATE TABLE `union_link_config` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `config_name` varchar(128) NOT NULL COMMENT '配置名称',

    -- 匹配维度
    `member_level` int(11) DEFAULT NULL COMMENT '推广者等级（NULL表示全部）',
    `platform` tinyint(4) DEFAULT NULL COMMENT '平台（NULL表示全部）',
    `category_id` bigint(20) DEFAULT NULL COMMENT '类目ID（NULL表示全部）',

    -- 转链策略
    `enable_short_url` tinyint(1) DEFAULT '1' COMMENT '是否生成短链接',
    `short_url_expire_days` int(11) DEFAULT '30' COMMENT '短链接有效期（天数）',
    `enable_promotion_code` tinyint(1) DEFAULT '1' COMMENT '是否生成口令/二维码',
    `enable_poster` tinyint(1) DEFAULT '0' COMMENT '是否生成海报',
    `auto_save_product` tinyint(1) DEFAULT '1' COMMENT '转链时自动保存商品信息到union_product',
    `click_tracking_enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用点击追踪',
    `smart_redirect` tinyint(1) DEFAULT '0' COMMENT '是否启用智能跳转（根据设备跳转不同端）',

    -- 限流配置
    `rate_limit_per_minute` int(11) DEFAULT '0' COMMENT '每分钟限流次数（0表示不限）',
    `rate_limit_per_hour` int(11) DEFAULT '0' COMMENT '每小时限流次数（0表示不限）',

    `priority` int(11) DEFAULT '0' COMMENT '优先级',
    `status` tinyint(4) DEFAULT '1' COMMENT '1-启用 0-禁用',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    KEY `idx_member_level` (`member_level`),
    KEY `idx_platform_category` (`platform`, `category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='转链配置表';


-- ------------------------------------------------------------
-- 提现申请表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `union_withdraw_apply`;
CREATE TABLE `union_withdraw_apply` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `apply_no` varchar(64) NOT NULL COMMENT '申请单号（业务唯一，格式：WD + 日期 + 序列号）',
    `member_id` bigint(20) NOT NULL COMMENT '推广者ID',
    `member_name` varchar(64) DEFAULT NULL COMMENT '推广者姓名（冗余，便于展示）',
    `member_mobile` varchar(20) DEFAULT NULL COMMENT '推广者手机号（冗余，便于联系）',

    -- 提现基本信息
    `apply_amount` decimal(12,2) NOT NULL COMMENT '申请提现金额',
    `actual_amount` decimal(12,2) DEFAULT NULL COMMENT '实际到账金额（扣除税费/手续费后）',
    `fee_amount` decimal(12,2) DEFAULT '0.00' COMMENT '手续费',
    `tax_amount` decimal(12,2) DEFAULT '0.00' COMMENT '代扣税费',
    `tax_rate` decimal(5,4) DEFAULT '0.0000' COMMENT '税费率（如0.05表示5%）',

    -- 提现方式
    `withdraw_type` tinyint(4) NOT NULL COMMENT '提现方式：1-银行卡 2-支付宝 3-微信支付',
    `pay_account` varchar(128) NOT NULL COMMENT '收款账号（脱敏存储）',
    `pay_account_name` varchar(64) NOT NULL COMMENT '收款人姓名',
    `pay_bank_name` varchar(128) DEFAULT NULL COMMENT '开户银行名称（银行卡时必填）',
    `pay_bank_branch` varchar(256) DEFAULT NULL COMMENT '开户支行名称',

    -- 状态机
    `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态：1-待审核 2-审核通过 3-已打款 4-已驳回 5-已取消 6-打款失败',

    -- 审核信息
    `auditor_id` bigint(20) DEFAULT NULL COMMENT '审核人ID',
    `auditor_name` varchar(64) DEFAULT NULL COMMENT '审核人姓名（冗余）',
    `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
    `audit_remark` varchar(256) DEFAULT NULL COMMENT '审核备注（驳回原因填这里）',

    -- 打款信息
    `pay_operator_id` bigint(20) DEFAULT NULL COMMENT '打款操作人ID',
    `pay_operator_name` varchar(64) DEFAULT NULL COMMENT '打款操作人姓名',
    `pay_time` datetime DEFAULT NULL COMMENT '打款时间',
    `pay_channel_order_no` varchar(128) DEFAULT NULL COMMENT '支付渠道订单号（如支付宝/微信流水号）',
    `pay_fail_reason` varchar(256) DEFAULT NULL COMMENT '打款失败原因',
    `retry_count` int(11) DEFAULT '0' COMMENT '打款重试次数',

    -- 关联资金流水
    `transaction_id` bigint(20) DEFAULT NULL COMMENT '关联资金流水ID（union_account_transaction.id）',
    `transaction_no` varchar(64) DEFAULT NULL COMMENT '关联资金流水号（冗余）',

    -- 用户操作记录
    `cancel_time` datetime DEFAULT NULL COMMENT '用户取消时间',
    `cancel_reason` varchar(256) DEFAULT NULL COMMENT '取消原因',

    -- 扩展字段
    `remark` varchar(256) DEFAULT NULL COMMENT '备注',
    `extra_json` json DEFAULT NULL COMMENT '扩展JSON（如：{"voucher_url": "xxx"}）',

    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_apply_no` (`apply_no`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_pay_channel_order` (`pay_channel_order_no`),
    KEY `idx_transaction_id` (`transaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提现申请表';


-- ------------------------------------------------------------
-- 提现配置表（系统级提现规则配置）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `union_withdraw_config`;
CREATE TABLE `union_withdraw_config` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `config_key` varchar(64) NOT NULL COMMENT '配置键',
    `config_name` varchar(128) NOT NULL COMMENT '配置名称',
    `config_value` varchar(512) NOT NULL COMMENT '配置值',
    `config_type` tinyint(4) NOT NULL COMMENT '配置类型：1-数字 2-字符串 3-百分比 4-JSON',
    `description` varchar(256) DEFAULT NULL COMMENT '配置说明',
    `is_active` tinyint(1) DEFAULT '1' COMMENT '是否启用',
    `sort_order` int(11) DEFAULT '0' COMMENT '排序',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提现配置表';


-- ------------------------------------------------------------
-- 提现审核日志表（记录每次审核操作）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `union_withdraw_audit_log`;
CREATE TABLE `union_withdraw_audit_log` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `apply_id` bigint(20) NOT NULL COMMENT '提现申请ID',
    `apply_no` varchar(64) NOT NULL COMMENT '申请单号',
    `operator_id` bigint(20) NOT NULL COMMENT '操作人ID',
    `operator_name` varchar(64) NOT NULL COMMENT '操作人姓名',
    `operation_type` tinyint(4) NOT NULL COMMENT '操作类型：1-提交申请 2-审核通过 3-审核驳回 4-打款 5-打款失败 6-用户取消 7-系统自动处理',
    `before_status` tinyint(4) DEFAULT NULL COMMENT '操作前状态',
    `after_status` tinyint(4) DEFAULT NULL COMMENT '操作后状态',
    `remark` varchar(512) DEFAULT NULL COMMENT '操作备注',
    `extra_json` json DEFAULT NULL COMMENT '扩展信息（如：{"auto_audit": true, "risk_score": 85}）',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_apply_id` (`apply_id`),
    KEY `idx_apply_no` (`apply_no`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提现审核日志表';


-- ------------------------------------------------------------
-- 提现风控记录表（记录风控检查结果）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `union_withdraw_risk_record`;
CREATE TABLE `union_withdraw_risk_record` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `apply_id` bigint(20) NOT NULL COMMENT '提现申请ID',
    `member_id` bigint(20) NOT NULL COMMENT '推广者ID',
    `risk_score` int(11) DEFAULT '0' COMMENT '风险评分（0-100，越高越危险）',
    `risk_level` tinyint(4) DEFAULT '1' COMMENT '风险等级：1-低 2-中 3-高 4-极高',
    `check_items` json DEFAULT NULL COMMENT '检查项明细：{"same_ip_orders": 10, "abnormal_amount": false, "new_device": true}',
    `hit_rules` json DEFAULT NULL COMMENT '命中的风控规则：["高频提现", "异常IP"]',
    `suggestion` varchar(256) DEFAULT NULL COMMENT '建议：PASS-通过 MANUAL-人工审核 REJECT-拒绝',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_apply_id` (`apply_id`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_risk_score` (`risk_score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提现风控记录表';


-- ------------------------------------------------------------
-- 风控规则配置表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `union_risk_rule_config`;
CREATE TABLE `union_risk_rule_config` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `rule_code` varchar(64) NOT NULL COMMENT '规则编码（唯一标识）',
    `rule_name` varchar(128) NOT NULL COMMENT '规则名称',
    `rule_category` tinyint(4) NOT NULL COMMENT '规则分类：1-用户行为 2-账户安全 3-交易异常 4-设备环境 5-业务合规',
    `rule_type` tinyint(4) NOT NULL COMMENT '规则类型：1-硬拒绝（直接拒绝） 2-软拒绝（转人工） 3-降级（限制额度） 4-仅记录（不打分）',

    -- 规则条件（JSON表达式，可配置复杂逻辑）
    `condition_expression` json NOT NULL COMMENT '条件表达式：{"field":"apply_amount","operator":">","value":50000}',
    `condition_sql` varchar(1024) DEFAULT NULL COMMENT '条件SQL片段（预编译，用于数据库过滤）',

    -- 规则动作
    `action_type` tinyint(4) NOT NULL COMMENT '动作类型：1-拒绝 2-转人工审核 3-限制提现额度 4-冻结账户 5-发送告警',
    `action_params` json DEFAULT NULL COMMENT '动作参数：{"limit_amount":1000, "freeze_days":7}',

    -- 评分权重
    `risk_score` int(11) DEFAULT '10' COMMENT '命中的风险分值（累加）',
    `auto_audit_threshold` int(11) DEFAULT '0' COMMENT '自动审核通过阈值（低于此值自动通过）',

    -- 优先级与状态
    `priority` int(11) DEFAULT '0' COMMENT '执行优先级（数字越小越先执行）',
    `status` tinyint(4) DEFAULT '1' COMMENT '1-启用 0-禁用',
    `start_time` datetime DEFAULT NULL COMMENT '生效开始时间',
    `end_time` datetime DEFAULT NULL COMMENT '生效结束时间',

    `description` varchar(256) DEFAULT NULL COMMENT '规则描述',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rule_code` (`rule_code`),
    KEY `idx_category_type` (`rule_category`, `rule_type`),
    KEY `idx_priority` (`priority`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控规则配置表';


-- ------------------------------------------------------------
-- 风控执行记录表（记录每次风控检查明细）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `union_risk_execution_log`;
CREATE TABLE `union_risk_execution_log` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `apply_id` bigint(20) NOT NULL COMMENT '提现申请ID',
    `member_id` bigint(20) NOT NULL COMMENT '推广者ID',

    -- 风控上下文
    `context_data` json NOT NULL COMMENT '风控上下文数据（所有检查字段的当前值）',

    -- 命中的规则
    `matched_rules` json DEFAULT NULL COMMENT '命中的规则列表：[{"rule_code":"R001","rule_name":"单日提现次数超限","risk_score":20}]',
    `total_risk_score` int(11) DEFAULT '0' COMMENT '总风险分值',

    -- 执行结果
    `decision` tinyint(4) NOT NULL COMMENT '决策：1-自动通过 2-转人工审核 3-拒绝 4-降额处理',
    `decision_reason` varchar(512) DEFAULT NULL COMMENT '决策原因',

    -- 降额信息（如有）
    `original_amount` decimal(12,2) DEFAULT NULL COMMENT '原始申请金额',
    `final_amount` decimal(12,2) DEFAULT NULL COMMENT '最终核定金额（降额后）',

    -- 执行耗时
    `execution_duration` int(11) DEFAULT '0' COMMENT '执行耗时（毫秒）',

    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    KEY `idx_apply_id` (`apply_id`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_decision` (`decision`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控执行记录表';

-- ------------------------------------------------------------
-- 风控黑名单表（统一管理IP、设备、用户）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `union_risk_blacklist`;
CREATE TABLE `union_risk_blacklist` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `black_type` tinyint(4) NOT NULL COMMENT '黑名单类型：1-IP 2-设备指纹 3-用户ID 4-手机号 5-身份证号',
    `black_value` varchar(128) NOT NULL COMMENT '黑名单值',
    `reason` varchar(256) DEFAULT NULL COMMENT '加入原因',
    `operator_id` bigint(20) DEFAULT NULL COMMENT '操作人ID',
    `expire_time` datetime DEFAULT NULL COMMENT '过期时间（NULL表示永久）',
    `status` tinyint(4) DEFAULT '1' COMMENT '1-生效 0-已失效',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_value` (`black_type`, `black_value`),
    KEY `idx_status_expire` (`status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控黑名单表';


-- ------------------------------------------------------------
-- 自动审核配置表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `union_auto_audit_config`;
CREATE TABLE `union_auto_audit_config` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `config_key` varchar(64) NOT NULL COMMENT '配置键',
    `config_value` varchar(64) NOT NULL COMMENT '配置值',
    `description` varchar(256) DEFAULT NULL COMMENT '描述',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动审核配置表';

-- ------------------------------------------------------------
-- 提现白名单表（白名单用户可享受更宽松的审核）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `union_withdraw_whitelist`;
CREATE TABLE `union_withdraw_whitelist` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `member_id` bigint(20) NOT NULL COMMENT '推广者ID',
    `whitelist_type` tinyint(4) NOT NULL COMMENT '类型：1-全部自动通过 2-大额豁免 3-高频豁免 4-免风控',
    `limit_amount` decimal(12,2) DEFAULT NULL COMMENT '额度上限（NULL表示不限制）',
    `reason` varchar(256) DEFAULT NULL COMMENT '加入原因',
    `operator_id` bigint(20) DEFAULT NULL COMMENT '操作人ID',
    `expire_time` datetime DEFAULT NULL COMMENT '过期时间（NULL表示永久）',
    `status` tinyint(4) DEFAULT '1' COMMENT '1-生效 0-已失效',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_member_type` (`member_id`, `whitelist_type`),
    KEY `idx_status_expire` (`status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提现白名单表';
