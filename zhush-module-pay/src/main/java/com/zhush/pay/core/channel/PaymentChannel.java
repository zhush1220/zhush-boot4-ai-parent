package com.zhush.pay.core.channel;

import com.zhush.pay.core.model.PayChannelType;
import com.zhush.pay.core.model.PaymentCommand;
import com.zhush.pay.core.model.PaymentResult;
import com.zhush.pay.core.model.RefundCommand;
import com.zhush.pay.core.model.RefundResult;

/**
 * 收款/退款渠道 SPI（三期）
 *
 * <p>与 {@link PayoutChannel} 对称：由 {@code PayGateway} 按 {@link #channelType()} 路由，
 * 实现类注册为 Bean 即自动纳入路由表（无实现时调用抛不支持异常）。
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

    /**
     * 退款查单（退款为异步，需据此收敛终态）
     */
    RefundResult queryRefund(String refundBizNo);

}
