package com.zhush.pay.wx.v2;

import cn.hutool.core.util.StrUtil;
import com.github.binarywang.wxpay.bean.notify.WxPayRefundNotifyResult;
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
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

import static com.zhush.common.exception.ServiceExceptionUtil.assertTrue;
import static com.zhush.common.exception.ServiceExceptionUtil.exception;

/**
 * 微信 V2 退款回调解析器
 *
 * <p>V2 退款通知的明文业务数据在 {@code req_info} 中，SDK 已用 APIv2 密钥自动解密，
 * {@link WxPayRefundNotifyResult#getReqInfo()} 直接可读。通知类型 {@link PayNotifyType#REFUND}，
 * bizNo 取商户退款单号（{@code out_refund_no}）。
 *
 * <p>{@code req_info.refund_status}：SUCCESS=退款成功，CHANGE=退款改成退款余额（视为失败），
 * 归一化交 {@link WxPayTradeStateMapper#toRefundStatus}。
 *
 * @author zhush
 */
@Slf4j
public class WxV2RefundNotifyParser implements PayNotifyParser {

    private static final DateTimeFormatter V2_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final WxPayService wxPayService;
    private final WxPayProperties properties;

    public WxV2RefundNotifyParser(WxPayService wxPayService, WxPayProperties properties) {
        this.wxPayService = wxPayService;
        this.properties = properties;
    }

    @Override
    public PayChannelType channelType() {
        return PayChannelType.WECHAT_V2;
    }

    @Override
    public PayNotifyType notifyType() {
        return PayNotifyType.REFUND;
    }

    @Override
    public PayNotifyMessage parse(String body, Map<String, String> headers) {
        assertTrue(StrUtil.isNotBlank(body), PayErrorCode.WX_PAY_NOTIFY_INVALID, "报文为空");

        WxPayRefundNotifyResult notify;
        try {
            notify = wxPayService.parseRefundNotifyResult(body);
        } catch (WxPayException e) {
            log.warn("[WxV2] 退款回调解析解密失败: {}", e.getMessage());
            throw exception(PayErrorCode.WX_PAY_NOTIFY_INVALID, "验签或解密失败");
        }
        WxPayRefundNotifyResult.ReqInfo reqInfo = notify == null ? null : notify.getReqInfo();
        assertTrue(reqInfo != null, PayErrorCode.WX_PAY_NOTIFY_INVALID, "req_info 解密结果为空");

        String expectedMchId = properties.getV2().getMchId();
        if (StrUtil.isNotBlank(expectedMchId) && StrUtil.isNotBlank(notify.getMchId())) {
            assertTrue(expectedMchId.equals(notify.getMchId()),
                    PayErrorCode.WX_PAY_NOTIFY_INVALID, "商户号不匹配");
        }

        PayStatus state = WxPayTradeStateMapper.toRefundStatus(reqInfo.getRefundStatus());
        return PayNotifyMessage.builder()
                .channelType(PayChannelType.WECHAT_V2)
                .notifyType(PayNotifyType.REFUND)
                .bizNo(reqInfo.getOutRefundNo())
                .channelOrderNo(reqInfo.getRefundId())
                .payStatus(state)
                .channelState(reqInfo.getRefundStatus())
                .amount(reqInfo.getRefundFee())
                .mchId(notify.getMchId())
                .successTime(parseTime(reqInfo.getSuccessTime()))
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
