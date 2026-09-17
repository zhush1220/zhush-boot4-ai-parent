package com.zhush.pay.core.channel;

import com.zhush.pay.core.model.PayChannelType;
import com.zhush.pay.core.model.PaymentCommand;
import com.zhush.pay.core.model.PaymentResult;
import com.zhush.pay.core.model.RefundCommand;
import com.zhush.pay.core.model.RefundResult;

/**
 * 收款/退款渠道 SPI —— <b>一期仅预留接口</b>，不实现。
 *
 * @author zhush
 */
public interface PaymentChannel {

    /**
     * 渠道唯一标识
     */
    PayChannelType channelType();

    /**
     * 发起收款
     */
    PaymentResult pay(PaymentCommand command);

    /**
     * 收款查单
     */
    PaymentResult query(String bizNo);

    /**
     * 发起退款
     */
    RefundResult refund(RefundCommand command);

}
