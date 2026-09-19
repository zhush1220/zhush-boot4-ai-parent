package com.zhush.pay.core.model;

import lombok.Builder;
import lombok.Data;

/**
 * 统一退款命令（协议无关）
 *
 * @author zhush
 */
@Data
@Builder
public class RefundCommand {

    /** 业务退款单号（渠道幂等键） */
    private String bizNo;

    /** 原支付业务单号 */
    private String originBizNo;

    /** 退款金额（单位：分） */
    private Integer amount;

    /** 渠道类型（协议） */
    private PayChannelType channelType;

    /** 退款原因 */
    private String reason;

    /** 结果回调地址 */
    private String notifyUrl;

}
