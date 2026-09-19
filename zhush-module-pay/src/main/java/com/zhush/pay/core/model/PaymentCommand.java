package com.zhush.pay.core.model;

import lombok.Builder;
import lombok.Data;

/**
 * 统一收款命令（协议无关）
 *
 * <p>用于订单收款（V3 JSAPI 等）。业务侧组装本命令调用 {@code PayGateway#pay}。
 *
 * @author zhush
 */
@Data
@Builder
public class PaymentCommand {

    /** 业务单号（渠道幂等键） */
    private String bizNo;

    /** 收款金额（单位：分） */
    private Integer amount;

    /** 渠道类型（协议） */
    private PayChannelType channelType;

    /** 付款方标识（如 JSAPI 的 openid） */
    private String payerId;

    /** 商品描述 */
    private String description;

    /** 结果回调地址（为空则用渠道默认配置） */
    private String notifyUrl;

    /** 微信 AppID（为空则用渠道默认配置；JSAPI 需与 openid 同主体） */
    private String appId;

}
