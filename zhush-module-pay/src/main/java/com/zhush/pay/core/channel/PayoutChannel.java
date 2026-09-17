package com.zhush.pay.core.channel;

import com.zhush.pay.core.model.PayChannelType;
import com.zhush.pay.core.model.PayoutCommand;
import com.zhush.pay.core.model.PayoutResult;

/**
 * 出款渠道 SPI
 *
 * <p>新增渠道（或新增协议）只需实现本接口并注册为 Spring Bean，
 * 由 {@code PayGateway} 按 {@link #channelType()} 自动路由，<b>无需改动业务代码</b>。
 *
 * <p><b>实现约定</b>：
 * <ul>
 *     <li>幂等：同一 {@code bizNo} 重复调用不得产生多笔渠道账；</li>
 *     <li><b>红线</b>：无渠道实现时必须抛异常，严禁返回 {@code accepted=true}；</li>
 *     <li><b>红线</b>：撤销仅当渠道明确返回「已撤销」才视为终态，撤销中不得解冻资金。</li>
 * </ul>
 *
 * @author zhush
 */
public interface PayoutChannel {

    /**
     * 渠道唯一标识
     */
    PayChannelType channelType();

    /**
     * 发起出款（幂等：同一 {@code bizNo}）
     */
    PayoutResult payout(PayoutCommand command);

    /**
     * 查单（V2 无回调时的唯一终态收敛手段）
     */
    PayoutResult query(String bizNo);

    /**
     * 撤单（仅部分渠道支持）
     */
    PayoutResult cancel(String bizNo);

    /**
     * 是否支持撤单
     */
    default boolean supportCancel() {
        return false;
    }

}
