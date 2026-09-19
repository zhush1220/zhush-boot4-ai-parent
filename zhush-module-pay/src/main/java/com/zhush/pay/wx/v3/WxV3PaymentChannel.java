package com.zhush.pay.wx.v3;

import cn.hutool.core.util.StrUtil;
import com.github.binarywang.wxpay.bean.request.WxPayRefundV3Request;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderV3Request;
import com.github.binarywang.wxpay.bean.result.WxPayOrderQueryV3Result;
import com.github.binarywang.wxpay.bean.result.WxPayRefundQueryV3Result;
import com.github.binarywang.wxpay.bean.result.WxPayRefundV3Result;
import com.github.binarywang.wxpay.bean.result.WxPayUnifiedOrderV3Result;
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
import com.zhush.pay.wx.exception.WxPayExceptionTranslator;
import lombok.extern.slf4j.Slf4j;

import static com.zhush.common.exception.ServiceExceptionUtil.assertTrue;

/**
 * 微信 V3 收款/退款渠道：JSAPI 下单 + 查单 + 退款
 *
 * <p>实现 {@link PaymentChannel}，由 {@code PayGateway} 按 {@link PayChannelType#WECHAT_V3} 路由。
 *
 * <p><b>红线</b>：
 * <ul>
 *     <li>下单成功仅代表「预支付已受理」，映射为 {@link PayStatus#PROCESSING}，绝不判成功到账；</li>
 *     <li>支付终态由 {@code queryOrderV3} 查单或支付回调收敛；</li>
 *     <li>退款为异步，受理后以 {@code PROCESSING} 返回，终态靠查单/退款回调。</li>
 * </ul>
 *
 * @author zhush
 */
@Slf4j
public class WxV3PaymentChannel implements PaymentChannel {

    private static final String CURRENCY_CNY = "CNY";

    private final WxPaymentClient client;
    private final WxPayProperties properties;

    public WxV3PaymentChannel(WxPaymentClient client, WxPayProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public PayChannelType channelType() {
        return PayChannelType.WECHAT_V3;
    }

    @Override
    public PaymentResult pay(PaymentCommand command) {
        validatePay(command);
        WxPayProperties.V3 v3 = properties.getV3();

        WxPayUnifiedOrderV3Request request = new WxPayUnifiedOrderV3Request();
        request.setAppid(StrUtil.blankToDefault(command.getAppId(), v3.getAppId()));
        request.setMchid(v3.getMchId());
        request.setDescription(command.getDescription());
        request.setOutTradeNo(command.getBizNo());
        request.setNotifyUrl(StrUtil.blankToDefault(command.getNotifyUrl(), properties.getNotifyUrl()));
        request.setAmount(new WxPayUnifiedOrderV3Request.Amount()
                .setTotal(command.getAmount())
                .setCurrency(CURRENCY_CNY));
        request.setPayer(new WxPayUnifiedOrderV3Request.Payer().setOpenid(command.getPayerId()));

        try {
            WxPayUnifiedOrderV3Result.JsapiResult jsapi = client.createJsapiOrder(request);
            String payParams = JacksonUtils.toJsonString(jsapi);
            log.info("[WxV3] JSAPI 下单受理, bizNo={}, prepayId={}", command.getBizNo(), jsapi.getPrepayId());
            return PaymentResult.accepted(PayStatus.PROCESSING, jsapi.getPrepayId(), payParams);
        } catch (WxPayException e) {
            log.error("[WxV3] JSAPI 下单失败, bizNo={}, reason={}", command.getBizNo(),
                    WxPayExceptionTranslator.describe(e));
            return PaymentResult.fail(WxPayExceptionTranslator.describe(e));
        }
    }

    @Override
    public PaymentResult query(String bizNo) {
        try {
            WxPayOrderQueryV3Result result = client.queryOrder(bizNo);
            PayStatus state = WxPayTradeStateMapper.toPayStatus(result.getTradeState());
            return PaymentResult.builder()
                    .accepted(true)
                    .finalState(state)
                    .channelOrderNo(result.getTransactionId())
                    .failReason(state == PayStatus.FAIL ? result.getTradeStateDesc() : null)
                    .build();
        } catch (WxPayException e) {
            // 查单调用失败：结果未知，accepted=false，业务侧不得据此判失败
            log.error("[WxV3] 支付查单失败, bizNo={}, reason={}", bizNo, WxPayExceptionTranslator.describe(e));
            return PaymentResult.fail(WxPayExceptionTranslator.describe(e));
        }
    }

    @Override
    public RefundResult refund(RefundCommand command) {
        validateRefund(command);
        try {
            // 退款需原交易单号与订单总额：先查原单（顺带校验已支付）
            WxPayOrderQueryV3Result origin = client.queryOrder(command.getOriginBizNo());
            assertTrue(origin.getTransactionId() != null && origin.getAmount() != null,
                    PayErrorCode.PAY_ORIGINAL_ORDER_MISSING, command.getOriginBizNo());

            WxPayRefundV3Request request = new WxPayRefundV3Request();
            request.setOutTradeNo(command.getOriginBizNo());
            request.setOutRefundNo(command.getBizNo());
            request.setReason(command.getReason());
            request.setNotifyUrl(StrUtil.blankToDefault(command.getNotifyUrl(), properties.getNotifyUrl()));
            request.setAmount(new WxPayRefundV3Request.Amount()
                    .setRefund(command.getAmount())
                    .setTotal(origin.getAmount().getTotal())
                    .setCurrency(CURRENCY_CNY));

            WxPayRefundV3Result result = client.refund(request);
            PayStatus state = WxPayTradeStateMapper.toRefundStatus(result.getStatus());
            log.info("[WxV3] 退款受理, refundBizNo={}, status={}, refundId={}",
                    command.getBizNo(), result.getStatus(), result.getRefundId());
            return RefundResult.builder()
                    .accepted(state != PayStatus.FAIL)
                    .finalState(state)
                    .channelOrderNo(result.getRefundId())
                    .failReason(state == PayStatus.FAIL ? "退款渠道返回状态：" + result.getStatus() : null)
                    .build();
        } catch (WxPayException e) {
            log.error("[WxV3] 退款调用失败, refundBizNo={}, reason={}", command.getBizNo(),
                    WxPayExceptionTranslator.describe(e));
            return RefundResult.fail(WxPayExceptionTranslator.describe(e));
        }
    }

    @Override
    public RefundResult queryRefund(String refundBizNo) {
        try {
            WxPayRefundQueryV3Result result = client.queryRefund(refundBizNo);
            PayStatus state = WxPayTradeStateMapper.toRefundStatus(result.getStatus());
            return RefundResult.builder()
                    .accepted(true)
                    .finalState(state)
                    .channelOrderNo(result.getRefundId())
                    .build();
        } catch (WxPayException e) {
            log.error("[WxV3] 退款查单失败, refundBizNo={}, reason={}", refundBizNo,
                    WxPayExceptionTranslator.describe(e));
            return RefundResult.fail(WxPayExceptionTranslator.describe(e));
        }
    }

    // ================ 校验 ================

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
