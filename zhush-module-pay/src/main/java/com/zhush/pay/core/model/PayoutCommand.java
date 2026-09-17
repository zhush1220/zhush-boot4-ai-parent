package com.zhush.pay.core.model;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * 统一出款命令（协议无关）
 *
 * <p>业务侧只需组装本命令调用 {@code PayGateway#payout}，不感知 V2/V3/微信。
 *
 * @author zhush
 */
@Data
@Builder
public class PayoutCommand {

    /** 业务单号（渠道幂等键；重试/并发必须复用同一值） */
    private String bizNo;

    /** 出款金额（单位：分） */
    private Integer amount;

    /** 渠道类型（协议） */
    private PayChannelType channelType;

    /** 收款方 */
    private Payee payee;

    /** 转账备注（用户可见） */
    private String remark;

    /** 结果回调地址（为空则用渠道默认配置） */
    private String notifyUrl;

    /** 微信 AppID（为空则用渠道默认配置） */
    private String appId;

    /** 商户号（为空则用渠道 primary；多商户路由预留） */
    private String mchId;

    /** 转账场景报备信息（V3 商家转账必填，如 {info_type: xxx, info_content: yyy}） */
    private Map<String, String> sceneReportInfos;

    /** 用户收款感知（V3：转账场景名称） */
    private String userRecvPerception;

}
