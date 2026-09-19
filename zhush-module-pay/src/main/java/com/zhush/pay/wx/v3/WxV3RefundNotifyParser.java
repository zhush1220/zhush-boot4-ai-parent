package com.zhush.pay.wx.v3;

import cn.hutool.core.util.StrUtil;
import com.github.binarywang.wxpay.bean.notify.SignatureHeader;
import com.github.binarywang.wxpay.bean.notify.WxPayRefundNotifyV3Result;
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
 * 微信 V3 退款回调解析器
 *
 * <p>通知类型 {@link PayNotifyType#REFUND}：bizNo 取商户退款单号（{@code out_refund_no}），
 * 与支付/转账回调按类型区分路由。
 *
 * @author zhush
 */
@Slf4j
public class WxV3RefundNotifyParser implements PayNotifyParser {

    private static final String HEADER_TIMESTAMP = "Wechatpay-Timestamp";
    private static final String HEADER_NONCE = "Wechatpay-Nonce";
    private static final String HEADER_SIGNATURE = "Wechatpay-Signature";
    private static final String HEADER_SERIAL = "Wechatpay-Serial";

    private final WxPayService wxPayService;
    private final WxPayProperties properties;

    public WxV3RefundNotifyParser(WxPayService wxPayService, WxPayProperties properties) {
        this.wxPayService = wxPayService;
        this.properties = properties;
    }

    @Override
    public PayChannelType channelType() {
        return PayChannelType.WECHAT_V3;
    }

    @Override
    public PayNotifyType notifyType() {
        return PayNotifyType.REFUND;
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

        WxPayRefundNotifyV3Result.DecryptNotifyResult result;
        try {
            result = wxPayService.parseRefundNotifyV3Result(body, signatureHeader).getResult();
        } catch (WxPayException e) {
            log.warn("[WxV3] 退款回调验签解密失败: {}", e.getMessage());
            throw exception(PayErrorCode.WX_PAY_NOTIFY_INVALID, "验签或解密失败");
        }
        assertTrue(result != null, PayErrorCode.WX_PAY_NOTIFY_INVALID, "解密结果为空");

        String expectedMchId = properties.getV3().getMchId();
        if (StrUtil.isNotBlank(expectedMchId) && StrUtil.isNotBlank(result.getMchid())) {
            assertTrue(expectedMchId.equals(result.getMchid()),
                    PayErrorCode.WX_PAY_NOTIFY_INVALID, "商户号不匹配");
        }

        PayStatus state = WxPayTradeStateMapper.toRefundStatus(result.getRefundStatus());
        return PayNotifyMessage.builder()
                .channelType(PayChannelType.WECHAT_V3)
                .notifyType(PayNotifyType.REFUND)
                .bizNo(result.getOutRefundNo())
                .channelOrderNo(result.getRefundId())
                .payStatus(state)
                .channelState(result.getRefundStatus())
                .mchId(result.getMchid())
                .successTime(parseTime(result.getSuccessTime()))
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
