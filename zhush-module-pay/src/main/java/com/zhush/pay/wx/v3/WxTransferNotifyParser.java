package com.zhush.pay.wx.v3;

import cn.hutool.core.util.StrUtil;
import com.github.binarywang.wxpay.bean.notify.SignatureHeader;
import com.github.binarywang.wxpay.bean.transfer.TransferBillsNotifyResult;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.zhush.pay.core.model.PayChannelType;
import com.zhush.pay.core.model.PayStatus;
import com.zhush.pay.core.notify.PayNotifyMessage;
import com.zhush.pay.core.notify.PayNotifyParser;
import com.zhush.pay.enums.PayErrorCode;
import com.zhush.pay.wx.WxPayProperties;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

import static com.zhush.common.exception.ServiceExceptionUtil.assertTrue;
import static com.zhush.common.exception.ServiceExceptionUtil.exception;

/**
 * 微信 V3 商家转账回调解析器
 *
 * <p>强制验签 + 解密（由 SDK 完成），再校验商户号归属，最终归一化为 {@link PayNotifyMessage}。
 *
 * @author zhush
 */
@Slf4j
public class WxTransferNotifyParser implements PayNotifyParser {

    private static final String HEADER_TIMESTAMP = "Wechatpay-Timestamp";
    private static final String HEADER_NONCE = "Wechatpay-Nonce";
    private static final String HEADER_SIGNATURE = "Wechatpay-Signature";
    private static final String HEADER_SERIAL = "Wechatpay-Serial";

    private final WxPayService wxPayService;
    private final WxPayProperties properties;

    public WxTransferNotifyParser(WxPayService wxPayService, WxPayProperties properties) {
        this.wxPayService = wxPayService;
        this.properties = properties;
    }

    @Override
    public PayChannelType channelType() {
        return PayChannelType.WECHAT_V3;
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

        TransferBillsNotifyResult notifyResult;
        try {
            notifyResult = wxPayService.getTransferService().parseTransferBillsNotifyResult(body, signatureHeader);
        } catch (WxPayException e) {
            // 验签/解密失败：不得视为可信报文
            log.warn("[WxV3] 回调验签解密失败: {}", e.getMessage());
            throw exception(PayErrorCode.WX_PAY_NOTIFY_INVALID, "验签或解密失败");
        }

        TransferBillsNotifyResult.DecryptNotifyResult result = notifyResult.getResult();
        assertTrue(result != null, PayErrorCode.WX_PAY_NOTIFY_INVALID, "解密结果为空");

        // 校验报文归属，防止串商户伪造
        String expectedMchId = properties.getV3().getMchId();
        if (StrUtil.isNotBlank(expectedMchId) && StrUtil.isNotBlank(result.getMchId())) {
            assertTrue(expectedMchId.equals(result.getMchId()),
                    PayErrorCode.WX_PAY_NOTIFY_INVALID, "商户号不匹配");
        }

        PayStatus state = WxTransferStateMapper.toPayStatus(result.getState());
        return PayNotifyMessage.builder()
                .channelType(PayChannelType.WECHAT_V3)
                .bizNo(result.getOutBillNo())
                .channelOrderNo(result.getTransferBillNo())
                .payStatus(state)
                .channelState(result.getState())
                .amount(result.getTransferAmount())
                .mchId(result.getMchId())
                .successTime(parseTime(result.getUpdateTime()))
                .failReason(result.getFailReason())
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
        // 兼容 header 名大小写差异
        return headers.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * 解析渠道时间（ISO-8601 带时区；异常时返回 null，不影响主流程）
     */
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
