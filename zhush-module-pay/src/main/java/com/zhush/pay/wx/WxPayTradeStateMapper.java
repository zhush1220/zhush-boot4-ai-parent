package com.zhush.pay.wx;

import com.zhush.pay.core.model.PayStatus;

/**
 * 微信 V2/V3 支付/退款状态 → 统一 {@link PayStatus} 映射（两版共用，{@code trade_state}/{@code refund_status} 词表一致）
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
     * <p>V3 取值：SUCCESS / CLOSED / PROCESSING / ABNORMAL；V2 refund_status：SUCCESS / CHANGE / REFUNDCLOSE
     */
    public static PayStatus toRefundStatus(String status) {
        if (status == null) {
            return PayStatus.PROCESSING;
        }
        return switch (status.toUpperCase()) {
            case "SUCCESS" -> PayStatus.SUCCESS;
            // V2 退款态：REFUNDCLOSE 视为失败终态；CHANGE（退款到零钱异常）需人工，保守非终态
            case "CLOSED", "ABNORMAL", "REFUNDCLOSE" -> PayStatus.FAIL;
            // PROCESSING / 其它：退款处理中，保持非终态
            default -> PayStatus.PROCESSING;
        };
    }

}
