package com.zhush.pay.core.model;

import lombok.Builder;
import lombok.Data;

/**
 * 统一收款命令（协议无关）—— <b>一期仅预留</b>，不实现。
 *
 * <p>未来用于订单收款（V2 统一下单 / V3 JSAPI 等）。
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

    /** 付款方标识（如 openid） */
    private String payerId;

    /** 商品描述 */
    private String description;

    /** 结果回调地址 */
    private String notifyUrl;

}
