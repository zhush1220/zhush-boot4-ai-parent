package com.zhush.union.enums;

import com.zhush.common.exception.IErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * union 模块错误码
 *
 * <p>模块号：{@code 003}
 * <ul>
 *     <li>{@code 1003xxx} —— 客户端错误</li>
 *     <li>{@code 2003xxx} —— 服务端错误</li>
 * </ul>
 *
 * @author zhush
 */
@Getter
@AllArgsConstructor
public enum UnionErrorCode implements IErrorCode {

    // ================ 商品/分类 1003 0xx ================
    PRODUCT_NOT_FOUND(1003001, "联盟商品主不存在：{0}"),
    CATEGORY_NOT_FOUND(1003002, "商品分类不存在：{0}"),
    CLICK_TRACKING_NOT_FOUND(1003003, "推广链接点击明细不存在：{0}"),
    PRODUCT_CATEGORY_NOT_FOUND(1003004, "商品分类关联不存在：{0}"),
    PRODUCT_EXTRA_NOT_FOUND(1003005, "商品扩展信息不存在：{0}"),
    PRODUCT_STATUS_LOG_NOT_FOUND(1003006, "商品状态变更日志不存在：{0}"),
    PRODUCT_SYNC_LOG_NOT_FOUND(1003007, "商品同步日志不存在：{0}"),
    PLATFORM_CATEGORY_MAP_NOT_FOUND(1003008, "平台类目映射不存在：{0}"),

    // ================ 转链/推广链接 1003 1xx ================
    PROMOTION_LINK_NOT_FOUND(1003101, "推广链接不存在：{0}"),
    PROMOTION_TEMPLATE_NOT_FOUND(1003102, "推广文案模板不存在：{0}"),
    LINK_CONFIG_NOT_FOUND(1003103, "转链配置不存在：{0}"),
    LINK_REQUEST_LOG_NOT_FOUND(1003104, "转链请求日志不存在：{0}"),
    SHORT_URL_MAP_NOT_FOUND(1003105, "短链接映射不存在：{0}"),

    // ================ 订单/归因 1003 2xx ================
    ORDER_NOT_FOUND(1003201, "联盟订单主不存在：{0}"),
    ORDER_ATTRIBUTION_LOG_NOT_FOUND(1003202, "订单归因日志不存在：{0}"),

    // ================ 佣金 1003 3xx ================
    COMMISSION_RECORD_NOT_FOUND(1003301, "佣金记录不存在：{0}"),
    COMMISSION_RULE_NOT_FOUND(1003302, "佣金分配规则配置不存在：{0}"),
    COMMISSION_ADJUSTMENT_NOT_FOUND(1003303, "佣金调整记录不存在：{0}"),
    COMMISSION_DISTRIBUTION_NOT_FOUND(1003304, "佣金分配明细不存在：{0}"),

    // ================ 外部推广 1003 4xx ================
    EXTERNAL_PROMOTER_NOT_FOUND(1003401, "外部推广者/影子推广者不存在：{0}"),
    EXTERNAL_ACCOUNT_NOT_FOUND(1003402, "外部推广者账户不存在：{0}"),
    EXTERNAL_COMMISSION_DETAIL_NOT_FOUND(1003403, "外部推广者佣金明细不存在：{0}"),
    EXTERNAL_COMMISSION_PENDING_NOT_FOUND(1003404, "外部推广者待结算佣金不存在：{0}"),
    EXTERNAL_TRANSACTION_NOT_FOUND(1003405, "外部推广者资金流水不存在：{0}"),
    EXTERNAL_WITHDRAW_NOT_FOUND(1003406, "外部推广者提现申请不存在：{0}"),

    // ================ 会员/层级/团队 1003 5xx ================
    MEMBER_ACCOUNT_NOT_FOUND(1003501, "推广者账户不存在：{0}"),
    MEMBER_DEVICE_NOT_FOUND(1003502, "推广者设备信息不存在：{0}"),
    MEMBER_LEVEL_NOT_FOUND(1003503, "推广者等级配置不存在：{0}"),
    MEMBER_RELATION_NOT_FOUND(1003504, "推广者关系不存在：{0}"),
    MEMBER_TEAM_SNAPSHOT_NOT_FOUND(1003505, "推广者团队快照不存在：{0}"),
    LEVEL_CONFIG_NOT_FOUND(1003506, "层级佣金配置不存在：{0}"),

    // ================ 账户资金/通知 1003 6xx ================
    ACCOUNT_TRANSACTION_NOT_FOUND(1003601, "账户资金流水不存在：{0}"),
    API_CALL_LOG_NOT_FOUND(1003602, "平台API调用日志不存在：{0}"),
    AUTO_AUDIT_CONFIG_NOT_FOUND(1003603, "自动审核配置不存在：{0}"),
    NOTIFICATION_NOT_FOUND(1003604, "消息通知不存在：{0}"),

    // ================ 提现/结算 1003 7xx ================
    WITHDRAW_APPLY_NOT_FOUND(1003701, "提现申请不存在：{0}"),
    WITHDRAW_AUDIT_LOG_NOT_FOUND(1003702, "提现审核日志不存在：{0}"),
    WITHDRAW_CONFIG_NOT_FOUND(1003703, "提现配置不存在：{0}"),
    WITHDRAW_RISK_RECORD_NOT_FOUND(1003704, "提现风控记录不存在：{0}"),
    WITHDRAW_WHITELIST_NOT_FOUND(1003705, "提现白名单不存在：{0}"),
    SETTLE_BATCH_NOT_FOUND(1003706, "结算批次不存在：{0}"),

    // ================ 风控 1003 8xx ================
    RISK_BLACKLIST_NOT_FOUND(1003801, "风控黑名单不存在：{0}"),
    RISK_RULE_CONFIG_NOT_FOUND(1003802, "风控规则配置不存在：{0}"),
    RISK_EXECUTION_LOG_NOT_FOUND(1003803, "风控执行记录不存在：{0}"),
    ;

    private final int code;
    private final String message;

}
