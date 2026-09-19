package com.zhush.pay.wx.v3;

import com.zhush.pay.core.model.PayStatus;

/**
 * 微信 V3 支付/退款状态 → 统一 {@link PayStatus} 映射
 *
 * <p>未知/中间态一律保守映射为 {@link PayStatus#PROCESSING}，<b>绝不</b>误判为成功或失败。
 *
 * @author zhush
 */
public final class WxPayTradeStateMapper {

    private WxPayTradeStateMapper() {
    }

    /**
     * 支付 trade_state → 统一状态
     *
     * <p>取值：SUCCESS / REFUND / NOTPAY / CLOSED / REVOKED / USERPAYING / PAYERROR
     */
    public static PayStatus toPayStatus(String tradeState) {
        if (tradeState == null) {
            return PayStatus.PROCESSING;
        }
        return switch (tradeState.toUpperCase()) {
            case "SUCCESS" -> PayStatus.SUCCESS;
            // 已收款后全额退款：对「支付」本身是成功终态
            case "REFUND" -> PayStatus.SUCCESS;
            case "CLOSED", "REVOKED", "PAYERROR" -> PayStatus.FAIL;
            // NOTPAY / USERPAYING / 其它：用户尚未完成支付，保持处理中
            default -> PayStatus.PROCESSING;
        };
    }

    /**
     * 退款 status → 统一状态
     *
     * <p>取值：SUCCESS / CLOSED / PROCESSING / ABNORMAL
     */
    public static PayStatus toRefundStatus(String status) {
        if (status == null) {
            return PayStatus.PROCESSING;
        }
        return switch (status.toUpperCase()) {
            case "SUCCESS" -> PayStatus.SUCCESS;
            case "CLOSED", "ABNORMAL" -> PayStatus.FAIL;
            // PROCESSING / 其它：退款处理中，保持非终态
            default -> PayStatus.PROCESSING;
        };
    }

}
