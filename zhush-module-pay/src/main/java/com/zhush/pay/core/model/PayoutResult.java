package com.zhush.pay.core.model;

import lombok.Builder;
import lombok.Data;

/**
 * 统一出款结果（协议无关）
 *
 * <p>区分两件事：
 * <ul>
 *     <li>{@link #accepted}：渠道是否<b>已受理</b>（不代表到账）；</li>
 *     <li>{@link #finalState}：渠道返回的<b>统一状态</b>，仅当 {@link PayStatus#isFinal()} 为真时才是终态。</li>
 * </ul>
 *
 * @author zhush
 */
@Data
@Builder
public class PayoutResult {

    /**
     * 渠道是否已应答：
     * <ul>
     *     <li>出款场景：true=已受理（可能仍是非终态），false=明确失败；</li>
     *     <li>查单场景：true=已取得渠道明确状态（成功/失败/处理中均算），false=查询调用失败、结果未知，
     *         业务侧必须保守处理，<b>不得</b>据此判定失败。</li>
     * </ul>
     */
    private boolean accepted;

    /** 统一状态 */
    private PayStatus finalState;

    /** 渠道订单号（V3：transferBillNo；V2：无则空） */
    private String channelOrderNo;

    /** 渠道原始状态字符串（脱敏，用于排查） */
    private String channelState;

    /** 收款用户确认用的 package 信息（V3 且 state=WAIT_USER_CONFIRM 时返回） */
    private String packageInfo;

    /** 失败原因 */
    private String failReason;

    public static PayoutResult accepted(PayStatus state, String channelOrderNo, String channelState) {
        return PayoutResult.builder()
                .accepted(true)
                .finalState(state)
                .channelOrderNo(channelOrderNo)
                .channelState(channelState)
                .build();
    }

    public static PayoutResult fail(String failReason) {
        return PayoutResult.builder()
                .accepted(false)
                .finalState(PayStatus.FAIL)
                .failReason(failReason)
                .build();
    }

}
