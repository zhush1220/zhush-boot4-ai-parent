package com.zhush.pay.core.notify;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 回调通知类型（业务维度）
 *
 * <p>微信 V3 的<b>商家转账</b>、<b>支付（收款）</b>、<b>退款</b>三类结果通知同属一个渠道
 * （{@code WECHAT_V3}）但走不同回调地址、报文结构各异。仅按 {@code PayChannelType} 路由会撞键，
 * 故引入本维度，与渠道类型组合成回调分发的复合键。
 *
 * @author zhush
 */
@Getter
@AllArgsConstructor
public enum PayNotifyType {

    /** 出款/转账结果（商家转账、企业付款） */
    TRANSFER("TRANSFER", "转账结果"),

    /** 收款（下单支付）结果 */
    PAYMENT("PAYMENT", "支付结果"),

    /** 退款结果 */
    REFUND("REFUND", "退款结果"),

    ;

    private final String code;
    private final String description;

}
