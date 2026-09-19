package com.zhush.pay.wx.v2;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.github.binarywang.wxpay.bean.order.WxPayMpOrderResult;
import com.github.binarywang.wxpay.bean.request.WxPayRefundRequest;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderRequest;
import com.github.binarywang.wxpay.bean.result.WxPayOrderQueryResult;
import com.github.binarywang.wxpay.bean.result.WxPayRefundQueryResult;
import com.github.binarywang.wxpay.bean.result.WxPayRefundResult;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.zhush.common.utils.json.JacksonUtils;
import com.zhush.pay.core.channel.PaymentChannel;
import com.zhush.pay.core.model.PayChannelType;
import com.zhush.pay.core.model.PayStatus;
import com.zhush.pay.core.model.PaymentCommand;
import com.zhush.pay.core.model.PaymentResult;
import com.zhush.pay.core.model.RefundCommand;
import com.zhush.pay.core.model.RefundResult;
import com.zhush.pay.enums.PayErrorCode;
import com.zhush.pay.wx.WxPayProperties;
import com.zhush.pay.wx.WxPayTradeStateMapper;
import com.zhush.pay.wx.exception.WxPayExceptionTranslator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

import static com.zhush.common.exception.ServiceExceptionUtil.assertTrue;

/**
 * 微信 V2 收款/退款渠道：统一下单（JSAPI）+ 查单 + 退款（含退差价）
 *
 * <p>实现 {@link PaymentChannel}，由 {@code PayGateway} 按 {@link PayChannelType#WECHAT_V3} 对偶的
 * {@link PayChannelType#WECHAT_V2} 路由。
 *
 * <p><b>红线</b>：下单成功仅代表受理（{@link PayStatus#PROCESSING}）；退款 {@code result_code=SUCCESS}
 * 仅代表受理，终态靠 {@code queryRefund}/退款回调收敛。V2 退款 {@code total_fee}/{@code refund_fee} 分离，
 * 天然支持退差价。
 *
 * @author zhush
 */
@Slf4j
public class WxV2PaymentChannel implements PaymentChannel {

    private static final String TRADE_TYPE_JSAPI = "JSAPI";
    private static final String PREPAY_ID_PREFIX = "prepay_id=";

    private final WxV2PaymentClient client;
    private final WxPayProperties properties;

    public WxV2PaymentChannel(WxV2PaymentClient client, WxPayProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public PayChannelType channelType() {
        return PayChannelType.WECHAT_V2;
    }

    @Override
    public PaymentResult pay(PaymentCommand command) {
        validatePay(command);
        WxPayProperties.V2 v2 = properties.getV2();

        WxPayUnifiedOrderRequest request = new WxPayUnifiedOrderRequest();
        request.setTradeType(TRADE_TYPE_JSAPI);
        request.setAppid(StrUtil.blankToDefault(command.getAppId(), v2.getAppId()));
        request.setMchId(v2.getMchId());
        request.setBody(command.getDescription());
        request.setOutTradeNo(command.getBizNo());
        request.setTotalFee(command.getAmount());
        request.setOpenid(command.getPayerId());
        request.setNotifyUrl(StrUtil.blankToDefault(command.getNotifyUrl(), properties.getNotifyUrl()));
        request.setSpbillCreateIp("127.0.0.1");

        try {
            WxPayMpOrderResult jsapi = client.createJsapiOrder(request);
            String payParams = JacksonUtils.toJsonString(jsapi);
            String prepayId = extractPrepayId(jsapi.getPackageValue());
            log.info("[WxV2] JSAPI 下单受理, bizNo={}, prepayId={}", command.getBizNo(), prepayId);
            return PaymentResult.accepted(PayStatus.PROCESSING, prepayId, payParams);
        } catch (WxPayException e) {
            log.error("[WxV2] JSAPI 下单失败, bizNo={}, reason={}", command.getBizNo(),
                    WxPayExceptionTranslator.describe(e));
            return PaymentResult.fail(WxPayExceptionTranslator.describe(e));
        }
    }

    @Override
    public PaymentResult query(String bizNo) {
        try {
            WxPayOrderQueryResult result = client.queryOrder(bizNo);
            PayStatus state = WxPayTradeStateMapper.toPayStatus(result.getTradeState());
            return PaymentResult.builder()
                    .accepted(true)
                    .finalState(state)
                    .channelOrderNo(result.getTransactionId())
                    .failReason(state == PayStatus.FAIL ? result.getTradeStateDesc() : null)
                    .build();
        } catch (WxPayException e) {
            // 查单调用失败：结果未知，accepted=false，业务侧不得据此判失败
            log.error("[WxV2] 支付查单失败, bizNo={}, reason={}", bizNo, WxPayExceptionTranslator.describe(e));
            return PaymentResult.fail(WxPayExceptionTranslator.describe(e));
        }
    }

    @Override
    public RefundResult refund(RefundCommand command) {
        validateRefund(command);
        try {
            // 退款需原单总额：现场查原单（顺带校验存在）
            WxPayOrderQueryResult origin = client.queryOrder(command.getOriginBizNo());
            assertTrue(origin.getTotalFee() != null && origin.getTransactionId() != null,
                    PayErrorCode.PAY_ORIGINAL_ORDER_MISSING, command.getOriginBizNo());

            WxPayRefundRequest request = new WxPayRefundRequest();
            request.setOutTradeNo(command.getOriginBizNo());
            request.setOutRefundNo(command.getBizNo());
            request.setTotalFee(origin.getTotalFee());                 // 原单总额（如 28600）
            request.setRefundFee(command.getAmount());                 // 本次退款额（如 8600）→ 退差价
            request.setRefundDesc(command.getReason());
            request.setNotifyUrl(StrUtil.blankToDefault(command.getNotifyUrl(), properties.getNotifyUrl()));

            WxPayRefundResult result = client.refund(request);
            boolean accepted = WxV2ResultChecker.businessSuccess(result);
            log.info("[WxV2] 退款受理, refundBizNo={}, accepted={}, refundId={}",
                    command.getBizNo(), accepted, result.getRefundId());
            return RefundResult.builder()
                    .accepted(accepted)
                    // V2 退款成功仅代表受理，终态一律非终态靠 queryRefund/回调收敛
                    .finalState(accepted ? PayStatus.PROCESSING : PayStatus.FAIL)
                    .channelOrderNo(result.getRefundId())
                    .failReason(accepted ? null : WxV2ResultChecker.failReason(result))
                    .build();
        } catch (WxPayException e) {
            log.error("[WxV2] 退款调用失败, refundBizNo={}, reason={}", command.getBizNo(),
                    WxPayExceptionTranslator.describe(e));
            return RefundResult.fail(WxPayExceptionTranslator.describe(e));
        }
    }

    @Override
    public RefundResult queryRefund(String refundBizNo) {
        try {
            WxPayRefundQueryResult result = client.queryRefund(refundBizNo);
            WxPayRefundQueryResult.RefundRecord record = matchRecord(result, refundBizNo);
            PayStatus state = WxPayTradeStateMapper.toRefundStatus(record == null ? null : record.getRefundStatus());
            return RefundResult.builder()
                    .accepted(true)
                    .finalState(state)
                    .channelOrderNo(record == null ? null : record.getRefundId())
                    .build();
        } catch (WxPayException e) {
            log.error("[WxV2] 退款查单失败, refundBizNo={}, reason={}", refundBizNo,
                    WxPayExceptionTranslator.describe(e));
            return RefundResult.fail(WxPayExceptionTranslator.describe(e));
        }
    }

    // ================ 内部方法 ================

    /**
     * 从 packageValue（{@code prepay_id=xxx}）提取预支付交易会话标识
     */
    private String extractPrepayId(String packageValue) {
        if (StrUtil.isBlank(packageValue)) {
            return null;
        }
        return packageValue.startsWith(PREPAY_ID_PREFIX)
                ? packageValue.substring(PREPAY_ID_PREFIX.length()) : packageValue;
    }

    /**
     * 退款查单结果按商户退款单号定位对应记录（一原单可能有多笔退款）
     */
    private WxPayRefundQueryResult.RefundRecord matchRecord(WxPayRefundQueryResult result, String refundBizNo) {
        List<WxPayRefundQueryResult.RefundRecord> records = result.getRefundRecords();
        if (CollUtil.isEmpty(records)) {
            return null;
        }
        return records.stream()
                .filter(r -> refundBizNo.equals(r.getOutRefundNo()))
                .findFirst()
                .orElse(records.size() == 1 ? records.get(0) : null);
    }

    private void validatePay(PaymentCommand command) {
        assertTrue(command != null && StrUtil.isNotBlank(command.getBizNo()),
                PayErrorCode.PAYEE_INVALID, "业务单号缺失");
        assertTrue(command.getAmount() != null && command.getAmount() > 0,
                PayErrorCode.PAY_AMOUNT_INVALID, command.getAmount());
        assertTrue(StrUtil.isNotBlank(command.getPayerId()),
                PayErrorCode.PAYEE_INVALID, "付款方 openid 缺失");
        assertTrue(StrUtil.isNotBlank(command.getDescription()),
                PayErrorCode.PAYEE_INVALID, "商品描述缺失");
    }

    private void validateRefund(RefundCommand command) {
        assertTrue(command != null && StrUtil.isNotBlank(command.getBizNo()),
                PayErrorCode.PAYEE_INVALID, "退款单号缺失");
        assertTrue(StrUtil.isNotBlank(command.getOriginBizNo()),
                PayErrorCode.PAY_ORIGINAL_ORDER_MISSING, "原支付单号缺失");
        assertTrue(command.getAmount() != null && command.getAmount() > 0,
                PayErrorCode.PAY_AMOUNT_INVALID, command.getAmount());
    }

}
