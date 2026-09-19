package com.zhush.pay.wx.v3;

import cn.hutool.core.util.StrUtil;
import com.github.binarywang.wxpay.bean.notify.SignatureHeader;
import com.github.binarywang.wxpay.bean.notify.WxPayNotifyV3Result;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.zhush.pay.core.model.PayChannelType;
import com.zhush.pay.core.model.PayStatus;
import com.zhush.pay.core.notify.PayNotifyMessage;
import com.zhush.pay.core.notify.PayNotifyParser;
import com.zhush.pay.core.notify.PayNotifyType;
import com.zhush.pay.enums.PayErrorCode;
import com.zhush.pay.wx.WxPayProperties;
import com.zhush.pay.wx.WxPayTradeStateMapper;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

import static com.zhush.common.exception.ServiceExceptionUtil.assertTrue;
import static com.zhush.common.exception.ServiceExceptionUtil.exception;

/**
 * 微信 V3 支付（收款）回调解析器
 *
 * <p>强制验签 + 解密（由 SDK 完成），校验商户号归属后归一化为 {@link PayNotifyMessage}。
 * 通知类型 {@link PayNotifyType#PAYMENT}，与转账回调按类型区分路由。
 *
 * @author zhush
 */
@Slf4j
public class WxV3PaymentNotifyParser implements PayNotifyParser {

    private static final String HEADER_TIMESTAMP = "Wechatpay-Timestamp";
    private static final String HEADER_NONCE = "Wechatpay-Nonce";
    private static final String HEADER_SIGNATURE = "Wechatpay-Signature";
    private static final String HEADER_SERIAL = "Wechatpay-Serial";

    private final WxPayService wxPayService;
    private final WxPayProperties properties;

    public WxV3PaymentNotifyParser(WxPayService wxPayService, WxPayProperties properties) {
        this.wxPayService = wxPayService;
        this.properties = properties;
    }

    @Override
    public PayChannelType channelType() {
        return PayChannelType.WECHAT_V3;
    }

    @Override
    public PayNotifyType notifyType() {
        return PayNotifyType.PAYMENT;
    }

    @Override
    public PayNotifyMessage parse(String body, Map<String, String> headers) {
        assertTrue(StrUtil.isNotBlank(body), PayErrorCode.WX_PAY_NOTIFY_INVALID, "报文为空");

        SignatureHeader signatureHeader = SignatureHeader.builder()
                .timeStamp(header(headers, HEADER_TIMESTAMP))
                .nonce(header(headers, HEADER_NONCE))
                .signature(header(headers, HEADER_SIGNATURE))
                .serial(header(headers, HEADER_SERIAL))
                .build();

        WxPayNotifyV3Result.DecryptNotifyResult result;
        try {
            result = wxPayService.parseOrderNotifyV3Result(body, signatureHeader).getResult();
        } catch (WxPayException e) {
            log.warn("[WxV3] 支付回调验签解密失败: {}", e.getMessage());
            throw exception(PayErrorCode.WX_PAY_NOTIFY_INVALID, "验签或解密失败");
        }
        assertTrue(result != null, PayErrorCode.WX_PAY_NOTIFY_INVALID, "解密结果为空");

        String expectedMchId = properties.getV3().getMchId();
        if (StrUtil.isNotBlank(expectedMchId) && StrUtil.isNotBlank(result.getMchid())) {
            assertTrue(expectedMchId.equals(result.getMchid()),
                    PayErrorCode.WX_PAY_NOTIFY_INVALID, "商户号不匹配");
        }

        PayStatus state = WxPayTradeStateMapper.toPayStatus(result.getTradeState());
        Integer amount = result.getAmount() == null ? null : result.getAmount().getPayerTotal();
        return PayNotifyMessage.builder()
                .channelType(PayChannelType.WECHAT_V3)
                .notifyType(PayNotifyType.PAYMENT)
                .bizNo(result.getOutTradeNo())
                .channelOrderNo(result.getTransactionId())
                .payStatus(state)
                .channelState(result.getTradeState())
                .amount(amount)
                .mchId(result.getMchid())
                .successTime(parseTime(result.getSuccessTime()))
                .failReason(state == PayStatus.FAIL ? result.getTradeStateDesc() : null)
                .build();
    }

    private String header(Map<String, String> headers, String name) {
        if (headers == null) {
            return null;
        }
        String value = headers.get(name);
        if (value != null) {
            return value;
        }
        return headers.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private LocalDateTime parseTime(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }

}
