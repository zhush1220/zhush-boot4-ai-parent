package com.zhush.pay.core.notify;

import com.zhush.pay.core.model.PayChannelType;

/**
 * 回调处理扩展点（业务侧实现）
 *
 * <p>业务侧实现本接口并注册为 Bean，由 {@link PayNotifyDispatcher} 按渠道类型路由；
 * 新增回调业务<b>无需</b>改动 pay。
 *
 * <p><b>约定</b>：处理失败必须抛异常（由控制器转 HTTP 500），以触发渠道重试。
 *
 * @author zhush
 */
public interface PayNotifyHandler {

    /**
     * 是否处理该渠道的回调
     */
    boolean supports(PayChannelType channelType);

    /**
     * 处理回调（幂等由实现方保证：按 bizNo + channelOrderNo 去重）
     */
    void handle(PayNotifyMessage message);

}
