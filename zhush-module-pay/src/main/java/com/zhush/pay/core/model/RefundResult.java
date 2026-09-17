package com.zhush.pay.core.model;

import lombok.Builder;
import lombok.Data;

/**
 * 统一退款结果（协议无关）—— <b>一期仅预留</b>，不实现。
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

}
