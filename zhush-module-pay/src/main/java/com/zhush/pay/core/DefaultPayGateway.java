package com.zhush.pay.core;

import com.zhush.pay.core.channel.PayoutChannel;
import com.zhush.pay.core.model.PayChannelType;
import com.zhush.pay.core.model.PayoutCommand;
import com.zhush.pay.core.model.PayoutResult;
import com.zhush.pay.enums.PayErrorCode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.zhush.common.exception.ServiceExceptionUtil.exception;

/**
 * 统一支付门面默认实现：按渠道类型路由到 {@link PayoutChannel} 实现
 *
 * <p>渠道实现由 Spring 自动聚合（{@code List<PayoutChannel>}），无需手工注册。
 * 未启用的渠道不在路由表中，调用即抛 {@link PayErrorCode#PAY_CHANNEL_UNSUPPORTED}——
 * <b>绝不</b>静默返回成功。
 *
 * @author zhush
 */
@Slf4j
@Component
public class DefaultPayGateway implements PayGateway {

    private final Map<PayChannelType, PayoutChannel> channelMap = new EnumMap<>(PayChannelType.class);

    public DefaultPayGateway(List<PayoutChannel> channels) {
        for (PayoutChannel channel : channels) {
            channelMap.put(channel.channelType(), channel);
        }
        log.info("[PayGateway] 已注册出款渠道: {}", channelMap.keySet());
    }

    @Override
    public PayoutResult payout(PayoutCommand command) {
        return resolve(command.getChannelType()).payout(command);
    }

    @Override
    public PayoutResult queryPayout(PayChannelType channelType, String bizNo) {
        return resolve(channelType).query(bizNo);
    }

    @Override
    public PayoutResult cancelPayout(PayChannelType channelType, String bizNo) {
        return resolve(channelType).cancel(bizNo);
    }

    @Override
    public boolean supportCancel(PayChannelType channelType) {
        return Optional.ofNullable(channelMap.get(channelType))
                .map(PayoutChannel::supportCancel)
                .orElse(false);
    }

    /**
     * 路由到渠道实现；未启用/未实现时抛业务异常（红线：不得返回 accepted）
     */
    private PayoutChannel resolve(PayChannelType channelType) {
        PayoutChannel channel = channelType == null ? null : channelMap.get(channelType);
        if (channel == null) {
            throw exception(PayErrorCode.PAY_CHANNEL_UNSUPPORTED, channelType);
        }
        return channel;
    }

}
