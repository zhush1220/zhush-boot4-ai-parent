package com.zhush.pay.core.notify;

import com.zhush.pay.core.model.PayChannelType;
import com.zhush.pay.enums.PayErrorCode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.zhush.common.exception.ServiceExceptionUtil.exception;

/**
 * 回调统一分发器
 *
 * <p>职责：按渠道类型选 {@link PayNotifyParser} 解析报文，再路由到业务侧
 * {@link PayNotifyHandler} 处理。业务侧不改本类即可接入新回调。
 *
 * @author zhush
 */
@Slf4j
@Component
public class PayNotifyDispatcher {

    private final Map<PayChannelType, PayNotifyParser> parserMap = new EnumMap<>(PayChannelType.class);
    private final List<PayNotifyHandler> handlers;

    public PayNotifyDispatcher(List<PayNotifyParser> parsers, List<PayNotifyHandler> handlers) {
        for (PayNotifyParser parser : parsers) {
            parserMap.put(parser.channelType(), parser);
        }
        this.handlers = handlers;
        log.info("[PayNotify] 已注册回调解析器: {}, 处理器数量: {}", parserMap.keySet(), handlers.size());
    }

    /**
     * 解析并分发回调
     *
     * @return 统一消息（供控制器记录）
     */
    public PayNotifyMessage dispatch(PayChannelType channelType, String body, Map<String, String> headers) {
        PayNotifyParser parser = parserMap.get(channelType);
        if (parser == null) {
            throw exception(PayErrorCode.PAY_CHANNEL_UNSUPPORTED, channelType);
        }
        PayNotifyMessage message = parser.parse(body, headers);
        for (PayNotifyHandler handler : handlers) {
            if (handler.supports(channelType)) {
                handler.handle(message);
                return message;
            }
        }
        throw exception(PayErrorCode.PAY_CHANNEL_UNSUPPORTED, channelType);
    }

}
