package com.zhush.pay.wx.v2;

import cn.hutool.core.util.StrUtil;
import com.github.binarywang.wxpay.bean.notify.WxPayOrderNotifyResult;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.zhush.pay.core.model.PayChannelType;
import com.zhush.pay.core.model.PayStatus;
import com.zhush.pay.core.notify.PayNotifyMessage;
import com.zhush.pay.core.notify.PayNotifyParser;
import com.zhush.pay.core.notify.PayNotifyType;
import com.zhush.pay.enums.PayErrorCode;
import com.zhush.pay.wx.WxPayProperties;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

import static com.zhush.common.exception.ServiceExceptionUtil.assertTrue;
import static com.zhush.common.exception.ServiceExceptionUtil.exception;

/**
 * 微信 V2 支付（收款）回调解析器
 *
 * <p>V2 通知为 XML，{@code parseOrderNotifyResult} 内部完成解析 + 验签（失败抛异常）。
 * 通知类型 {@link PayNotifyType#PAYMENT}，与 V3/退款回调按 (channelType, notifyType) 复合键路由。
 *
 * <p><b>V2 特性</b>：支付成功通知不含 {@code trade_state}，仅支付成功时推送——
 * 验签通过的报文即视为 {@link PayStatus#SUCCESS}（终态）；如需精确态请配合查单。
 *
 * @author zhush
 */
@Slf4j
public class WxV2PaymentNotifyParser implements PayNotifyParser {

    /** V2 时间格式：yyyyMMddHHmmss */
    private static final DateTimeFormatter V2_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final WxPayService wxPayService;
    private final WxPayProperties properties;

    public WxV2PaymentNotifyParser(WxPayService wxPayService, WxPayProperties properties) {
        this.wxPayService = wxPayService;
        this.properties = properties;
    }

    @Override
    public PayChannelType channelType() {
        return PayChannelType.WECHAT_V2;
    }

    @Override
    public PayNotifyType notifyType() {
        return PayNotifyType.PAYMENT;
    }

    @Override
    public PayNotifyMessage parse(String body, Map<String, String> headers) {
        assertTrue(StrUtil.isNotBlank(body), PayErrorCode.WX_PAY_NOTIFY_INVALID, "报文为空");

        WxPayOrderNotifyResult result;
        try {
            result = wxPayService.parseOrderNotifyResult(body);
        } catch (WxPayException e) {
            log.warn("[WxV2] 支付回调解析验签失败: {}", e.getMessage());
            throw exception(PayErrorCode.WX_PAY_NOTIFY_INVALID, "验签失败");
        }
        assertTrue(result != null, PayErrorCode.WX_PAY_NOTIFY_INVALID, "解析结果为空");

        String expectedMchId = properties.getV2().getMchId();
        if (StrUtil.isNotBlank(expectedMchId) && StrUtil.isNotBlank(result.getMchId())) {
            assertTrue(expectedMchId.equals(result.getMchId()),
                    PayErrorCode.WX_PAY_NOTIFY_INVALID, "商户号不匹配");
        }

        return PayNotifyMessage.builder()
                .channelType(PayChannelType.WECHAT_V2)
                .notifyType(PayNotifyType.PAYMENT)
                .bizNo(result.getOutTradeNo())
                .channelOrderNo(result.getTransactionId())
                // V2 成功通知即支付成功（无 trade_state）
                .payStatus(PayStatus.SUCCESS)
                .channelState("SUCCESS")
                .amount(result.getTotalFee())
                .mchId(result.getMchId())
                .successTime(parseTime(result.getTimeEnd()))
                .build();
    }

    private LocalDateTime parseTime(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, V2_TIME);
        } catch (Exception e) {
            return null;
        }
    }

}
