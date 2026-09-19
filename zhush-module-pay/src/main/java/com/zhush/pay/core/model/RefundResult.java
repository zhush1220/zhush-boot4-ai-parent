package com.zhush.pay.core.model;

import lombok.Builder;
import lombok.Data;

/**
 * 统一退款结果（协议无关）
 *
 * @author zhush
 */
@Data
@Builder
public class RefundResult {

    /** 渠道是否已受理 */
    private boolean accepted;

    /** 统一状态 */
    private PayStatus finalState;

    /** 渠道退款单号 */
    private String channelOrderNo;

    /** 失败原因 */
    private String failReason;

    /** 退款已受理/已查得明确状态（以 finalState 为准是否终态） */
    public static RefundResult of(PayStatus state, String channelOrderNo) {
        return RefundResult.builder()
                .accepted(true)
                .finalState(state)
                .channelOrderNo(channelOrderNo)
                .build();
    }

    /** 明确失败（未受理） */
    public static RefundResult fail(String failReason) {
        return RefundResult.builder()
                .accepted(false)
                .finalState(PayStatus.FAIL)
                .failReason(failReason)
                .build();
    }

}
