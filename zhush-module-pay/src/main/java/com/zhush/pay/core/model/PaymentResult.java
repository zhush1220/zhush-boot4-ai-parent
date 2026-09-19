package com.zhush.pay.core.model;

import lombok.Builder;
import lombok.Data;

/**
 * 统一收款结果（协议无关）
 *
 * @author zhush
 */
@Data
@Builder
public class PaymentResult {

    /** 渠道是否已受理 */
    private boolean accepted;

    /** 统一状态 */
    private PayStatus finalState;

    /** 渠道订单号 */
    private String channelOrderNo;

    /** 拉起支付的参数（如 JSAPI 的 prepay_id 及签名包） */
    private String payParams;

    /** 失败原因 */
    private String failReason;

    /** 下单已受理（非终态），携带拉起支付参数 */
    public static PaymentResult accepted(PayStatus state, String channelOrderNo, String payParams) {
        return PaymentResult.builder()
                .accepted(true)
                .finalState(state)
                .channelOrderNo(channelOrderNo)
                .payParams(payParams)
                .build();
    }

    /** 明确失败（未受理） */
    public static PaymentResult fail(String failReason) {
        return PaymentResult.builder()
                .accepted(false)
                .finalState(PayStatus.FAIL)
                .failReason(failReason)
                .build();
    }

}
