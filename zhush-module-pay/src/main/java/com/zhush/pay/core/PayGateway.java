package com.zhush.pay.core;

import com.zhush.pay.core.model.PayChannelType;
import com.zhush.pay.core.model.PayoutCommand;
import com.zhush.pay.core.model.PayoutResult;

/**
 * 统一支付门面（协议无关）
 *
 * <p>业务侧只依赖本接口：按 {@link PayoutCommand#getChannelType()} 自动路由到对应
 * {@code PayoutChannel} 实现，业务代码不感知 V2/V3/微信。
 *
 * <p>新增业务（分润出款、会员提现…）无需改动 pay；新增渠道只需实现渠道 SPI。
 *
 * @author zhush
 */
public interface PayGateway {

    /**
     * 发起出款（幂等：同一 {@code bizNo} 只产生一笔渠道账）
     */
    PayoutResult payout(PayoutCommand command);

    /**
     * 查询出款结果（终态收敛；V2 无回调时唯一手段）
     */
    PayoutResult queryPayout(PayChannelType channelType, String bizNo);

    /**
     * 撤销出款（返回状态为终态「已撤销」时方可解冻/回滚）
     */
    PayoutResult cancelPayout(PayChannelType channelType, String bizNo);

    /**
     * 指定渠道是否支持撤单
     */
    boolean supportCancel(PayChannelType channelType);

}
