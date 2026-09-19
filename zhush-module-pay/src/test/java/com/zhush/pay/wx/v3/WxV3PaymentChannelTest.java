package com.zhush.pay.wx.v3;

import com.github.binarywang.wxpay.bean.request.WxPayRefundV3Request;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderV3Request;
import com.github.binarywang.wxpay.bean.result.WxPayOrderQueryV3Result;
import com.github.binarywang.wxpay.bean.result.WxPayRefundQueryV3Result;
import com.github.binarywang.wxpay.bean.result.WxPayRefundV3Result;
import com.github.binarywang.wxpay.bean.result.WxPayUnifiedOrderV3Result;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.zhush.common.exception.ServiceException;
import com.zhush.pay.core.model.PayChannelType;
import com.zhush.pay.core.model.PayStatus;
import com.zhush.pay.core.model.PaymentCommand;
import com.zhush.pay.core.model.PaymentResult;
import com.zhush.pay.core.model.RefundCommand;
import com.zhush.pay.core.model.RefundResult;
import com.zhush.pay.wx.WxPayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 微信 V3 收款/退款渠道契约测试（Mock 协议层）
 *
 * <p>验证资金安全红线：下单受理仅 PROCESSING；查单/退款按渠道状态归一；调用异常保守返回非成功。
 */
@ExtendWith(MockitoExtension.class)
class WxV3PaymentChannelTest {

    @Mock
    private WxPaymentClient client;
    @Mock
    private WxPayProperties properties;
    @Mock
    private WxPayProperties.V3 v3;

    private WxV3PaymentChannel channel;

    @BeforeEach
    void setUp() {
        channel = new WxV3PaymentChannel(client, properties);
    }

    private PaymentCommand payCommand() {
        return PaymentCommand.builder()
                .bizNo("PAY20260919001")
                .amount(1)
                .channelType(PayChannelType.WECHAT_V3)
                .payerId("openid-abc")
                .description("测试商品")
                .build();
    }

    private RefundCommand refundCommand() {
        return RefundCommand.builder()
                .bizNo("RF20260919001")
                .originBizNo("PAY20260919001")
                .amount(1)
                .channelType(PayChannelType.WECHAT_V3)
                .reason("用户取消")
                .build();
    }

    // ==================== pay ====================

    @Test
    void pay_acceptedButNotFinalState() throws WxPayException {
        when(properties.getV3()).thenReturn(v3);
        WxPayUnifiedOrderV3Result.JsapiResult jsapi = new WxPayUnifiedOrderV3Result.JsapiResult();
        jsapi.setPrepayId("wx_prepay_123");
        when(client.createJsapiOrder(any(WxPayUnifiedOrderV3Request.class))).thenReturn(jsapi);

        PaymentResult result = channel.pay(payCommand());

        // 下单成功仅「受理」，绝不能判资金到账
        assertTrue(result.isAccepted());
        assertEquals(PayStatus.PROCESSING, result.getFinalState());
        assertEquals("wx_prepay_123", result.getChannelOrderNo());
        assertNotNull(result.getPayParams());
    }

    @Test
    void pay_channelError_returnsNotAccepted() throws WxPayException {
        when(properties.getV3()).thenReturn(v3);
        when(client.createJsapiOrder(any(WxPayUnifiedOrderV3Request.class)))
                .thenThrow(new WxPayException("下单失败"));

        PaymentResult result = channel.pay(payCommand());

        assertFalse(result.isAccepted());
        assertEquals(PayStatus.FAIL, result.getFinalState());
        assertNotNull(result.getFailReason());
    }

    @Test
    void pay_missingAmount_throws() {
        PaymentCommand command = payCommand();
        command.setAmount(null);
        assertThrows(ServiceException.class, () -> channel.pay(command));
    }

    // ==================== query ====================

    @Test
    void query_successState() throws WxPayException {
        WxPayOrderQueryV3Result query = mock(WxPayOrderQueryV3Result.class);
        when(query.getTradeState()).thenReturn("SUCCESS");
        when(query.getTransactionId()).thenReturn("TX1001");
        when(client.queryOrder("PAY20260919001")).thenReturn(query);

        PaymentResult result = channel.query("PAY20260919001");

        assertTrue(result.isAccepted());
        assertEquals(PayStatus.SUCCESS, result.getFinalState());
        assertEquals("TX1001", result.getChannelOrderNo());
    }

    @Test
    void query_closedStateCarriesFailReason() throws WxPayException {
        WxPayOrderQueryV3Result query = mock(WxPayOrderQueryV3Result.class);
        when(query.getTradeState()).thenReturn("CLOSED");
        when(query.getTransactionId()).thenReturn(null);
        when(query.getTradeStateDesc()).thenReturn("订单已关闭");
        when(client.queryOrder("PAY20260919001")).thenReturn(query);

        PaymentResult result = channel.query("PAY20260919001");

        assertEquals(PayStatus.FAIL, result.getFinalState());
        assertEquals("订单已关闭", result.getFailReason());
    }

    @Test
    void query_callError_returnsUnknownNotFail() throws WxPayException {
        when(client.queryOrder("PAY20260919001")).thenThrow(new WxPayException("网络超时"));

        PaymentResult result = channel.query("PAY20260919001");

        // 查单调用失败：结果未知，accepted=false，业务侧不得据此判失败
        assertFalse(result.isAccepted());
    }

    // ==================== refund ====================

    @Test
    void refund_queriesOriginThenAccepts() throws WxPayException {
        WxPayOrderQueryV3Result origin = mock(WxPayOrderQueryV3Result.class);
        WxPayOrderQueryV3Result.Amount originAmount = mock(WxPayOrderQueryV3Result.Amount.class);
        when(origin.getTransactionId()).thenReturn("TX1001");
        when(origin.getAmount()).thenReturn(originAmount);
        when(originAmount.getTotal()).thenReturn(100);
        when(client.queryOrder("PAY20260919001")).thenReturn(origin);

        WxPayRefundV3Result refund = mock(WxPayRefundV3Result.class);
        when(refund.getStatus()).thenReturn("SUCCESS");
        when(refund.getRefundId()).thenReturn("RF-ID-1");
        when(client.refund(any(WxPayRefundV3Request.class))).thenReturn(refund);

        RefundResult result = channel.refund(refundCommand());

        assertTrue(result.isAccepted());
        assertEquals(PayStatus.SUCCESS, result.getFinalState());
        assertEquals("RF-ID-1", result.getChannelOrderNo());
    }

    @Test
    void refund_processingStatusStaysNonFinal() throws WxPayException {
        WxPayOrderQueryV3Result origin = mock(WxPayOrderQueryV3Result.class);
        WxPayOrderQueryV3Result.Amount originAmount = mock(WxPayOrderQueryV3Result.Amount.class);
        when(origin.getTransactionId()).thenReturn("TX1001");
        when(origin.getAmount()).thenReturn(originAmount);
        when(originAmount.getTotal()).thenReturn(100);
        when(client.queryOrder("PAY20260919001")).thenReturn(origin);

        WxPayRefundV3Result refund = mock(WxPayRefundV3Result.class);
        when(refund.getStatus()).thenReturn("PROCESSING");
        when(refund.getRefundId()).thenReturn("RF-ID-1");
        when(client.refund(any(WxPayRefundV3Request.class))).thenReturn(refund);

        RefundResult result = channel.refund(refundCommand());

        // 退款受理非终态，需靠查单/回调收敛
        assertTrue(result.isAccepted());
        assertEquals(PayStatus.PROCESSING, result.getFinalState());
    }

    @Test
    void refund_originOrderMissing_throws() throws WxPayException {
        WxPayOrderQueryV3Result origin = mock(WxPayOrderQueryV3Result.class);
        when(origin.getTransactionId()).thenReturn(null);
        when(client.queryOrder("PAY20260919001")).thenReturn(origin);

        assertThrows(ServiceException.class, () -> channel.refund(refundCommand()));
    }

    // ==================== queryRefund ====================

    @Test
    void queryRefund_successState() throws WxPayException {
        WxPayRefundQueryV3Result query = mock(WxPayRefundQueryV3Result.class);
        when(query.getStatus()).thenReturn("SUCCESS");
        when(query.getRefundId()).thenReturn("RF-ID-1");
        when(client.queryRefund("RF20260919001")).thenReturn(query);

        RefundResult result = channel.queryRefund("RF20260919001");

        assertTrue(result.isAccepted());
        assertEquals(PayStatus.SUCCESS, result.getFinalState());
        assertEquals("RF-ID-1", result.getChannelOrderNo());
    }

}
