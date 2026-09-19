package com.zhush.pay.enums;

import com.zhush.common.exception.IErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * pay 模块错误码
 *
 * <p>模块号：{@code 004}
 * <ul>
 *     <li>{@code 1004xxx} —— 客户端/参数错误</li>
 *     <li>{@code 2004xxx} —— 服务端/渠道错误</li>
 * </ul>
 *
 * @author zhush
 */
@Getter
@AllArgsConstructor
public enum PayErrorCode implements IErrorCode {

    // ================ 参数/路由 1004 0xx ================
    PAY_CHANNEL_UNSUPPORTED(1004001, "不支持的支付渠道：{0}"),
    PAY_AMOUNT_INVALID(1004002, "出款金额非法：{0}"),
    PAYEE_INVALID(1004003, "收款方信息不完整：{0}"),
    PAY_NOT_SUPPORT_CANCEL(1004004, "该渠道不支持撤单：{0}"),

    // ================ 配置 1004 1xx ================
    WX_PAY_CONFIG_MISSING(1004101, "微信支付配置缺失：{0}"),
    WX_PAY_NOT_ENABLED(1004102, "微信支付渠道未启用"),

    // ================ 渠道调用 2004 0xx ================
    WX_PAY_CALL_FAIL(2004001, "微信支付调用失败：{0}"),
    WX_PAY_NOTIFY_INVALID(2004002, "微信支付回调报文非法：{0}"),
    PAYOUT_FAILED(2004003, "渠道出款失败：{0}"),
    PAY_ORIGINAL_ORDER_MISSING(2004004, "原支付订单不存在或状态不可退款：{0}"),
    ;

    private final int code;
    private final String message;

}
