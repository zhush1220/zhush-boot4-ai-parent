package com.zhush.pay.core.notify;

import com.zhush.pay.core.model.PayChannelType;
import java.util.Map;

/**
 * 回调解析 SPI（协议无关入口）
 *
 * <p>各协议实现负责：验签 + 解密 + 归一化为 {@link PayNotifyMessage}。
 * <b>验签失败必须抛异常</b>，不得返回「可信」消息。
 *
 * @author zhush
 */
public interface PayNotifyParser {

    /**
     * 支持的渠道类型
     */
    PayChannelType channelType();

    /**
     * 解析回调报文（验签 + 解密），并归一化为统一消息
     *
     * @param body    原始报文（V3 为 JSON 密文；V2 为 XML）
     * @param headers 请求头（V3 需用到 Wechatpay-Timestamp/Nonce/Signature/Serial）
     */
    PayNotifyMessage parse(String body, Map<String, String> headers);

}
