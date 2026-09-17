package com.zhush.pay.core.model;

import lombok.Builder;
import lombok.Data;

/**
 * 统一收款结果（协议无关）—— <b>一期仅预留</b>，不实现。
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

}
