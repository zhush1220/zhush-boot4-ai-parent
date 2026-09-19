package com.zhush.pay.wx.v2;

import com.github.binarywang.wxpay.bean.order.WxPayMpOrderResult;
import com.github.binarywang.wxpay.bean.request.WxPayRefundRequest;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderRequest;
import com.github.binarywang.wxpay.bean.result.WxPayOrderQueryResult;
import com.github.binarywang.wxpay.bean.result.WxPayRefundQueryResult;
import com.github.binarywang.wxpay.bean.result.WxPayRefundResult;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.zhush.common.exception.ServiceException;
import com.zhush.pay.core.model.PayChannelType;
import com.zhush.pay.core.model.PayStatus;
import com.zhush.pay.core.model.PaymentCommand;
import com.zhush.pay.core.model.PaymentResult;
import com.zhush.pay.core.model.RefundCommand;
import com.zhush.pay.core.model.RefundResult;
import com.zhush.pay.wx.WxPayProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 微信 V2 收款/退款渠道契约测试（Mock 协议层）
 *
 * <p>重点验证：下单受理仅 PROCESSING；<b>退差价</b>的 total_fee/refund_fee 分离；退款受理非终态。
 */
@ExtendWith(MockitoExtension.class)
class WxV2PaymentChannelTest {

    @Mock
    private WxV2PaymentClient client;
    @Mock
    private WxPayProperties properties;
    @Mock
    private WxPayProperties.V2 v2;

    private WxV2PaymentChannel channel;

    @BeforeEach
    void setUp() {
        channel = new WxV2PaymentChannel(client, properties);
    }

    private PaymentCommand payCommand() {
        return PaymentCommand.builder()
                .bizNo("PAY20260919001")
                .amount(28600)
                .channelType(PayChannelType.WECHAT_V2)
                .payerId("openid-abc")
                .description("测试商品")
                .build();
    }

    private RefundCommand refundCommand() {
        return RefundCommand.builder()
                .bizNo("RF20260919001")
                .originBizNo("PAY20260919001")
                .amount(8600)
                .channelType(PayChannelType.WECHAT_V2)
                .reason("价格调整退差价")
                .build();
    }

    // ==================== pay ====================

    @Test
    void pay_acceptedButNotFinalState() throws WxPayException {
        when(properties.getV2()).thenReturn(v2);
        WxPayMpOrderResult jsapi = new WxPayMpOrderResult();
        jsapi.setPackageValue("prepay_id=wx_prepay_123");
        when(client.createJsapiOrder(any(WxPayUnifiedOrderRequest.class))).thenReturn(jsapi);

        PaymentResult result = channel.pay(payCommand());

        assertTrue(result.isAccepted());
        assertEquals(PayStatus.PROCESSING, result.getFinalState());
        assertEquals("wx_prepay_123", result.getChannelOrderNo());
        assertNotNull(result.getPayParams());
    }

    @Test
    void pay_channelError_returnsNotAccepted() throws WxPayException {
        when(properties.getV2()).thenReturn(v2);
        when(client.createJsapiOrder(any(WxPayUnifiedOrderRequest.class)))
                .thenThrow(new WxPayException("下单失败"));

        PaymentResult result = channel.pay(payCommand());

        assertFalse(result.isAccepted());
        assertEquals(PayStatus.FAIL, result.getFinalState());
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
        WxPayOrderQueryResult query = mock(WxPayOrderQueryResult.class);
        when(query.getTradeState()).thenReturn("SUCCESS");
        when(query.getTransactionId()).thenReturn("TX1001");
        when(client.queryOrder("PAY20260919001")).thenReturn(query);

        PaymentResult result = channel.query("PAY20260919001");

        assertTrue(result.isAccepted());
        assertEquals(PayStatus.SUCCESS, result.getFinalState());
        assertEquals("TX1001", result.getChannelOrderNo());
    }

    @Test
    void query_callError_returnsUnknownNotFail() throws WxPayException {
        when(client.queryOrder("PAY20260919001")).thenThrow(new WxPayException("网络超时"));

        PaymentResult result = channel.query("PAY20260919001");

        assertFalse(result.isAccepted());
    }

    // ==================== refund：退差价核心 ====================

    @Test
    void refundDifference_setsTotalAndRefundFeeSeparately() throws WxPayException {
        // 原单实付 28600
        WxPayOrderQueryResult origin = mock(WxPayOrderQueryResult.class);
        when(origin.getTotalFee()).thenReturn(28600);
        when(origin.getTransactionId()).thenReturn("TX1001");
        when(client.queryOrder("PAY20260919001")).thenReturn(origin);

        WxPayRefundResult refund = mock(WxPayRefundResult.class);
        when(refund.getReturnCode()).thenReturn("SUCCESS");
        when(refund.getResultCode()).thenReturn("SUCCESS");
        when(refund.getRefundId()).thenReturn("RF-ID-1");
        when(client.refund(any(WxPayRefundRequest.class))).thenReturn(refund);

        RefundResult result = channel.refund(refundCommand());

        // 断言渠道请求：total_fee=原单总额 28600、refund_fee=本次 8600（退差价语义）
        ArgumentCaptor<WxPayRefundRequest> captor = ArgumentCaptor.forClass(WxPayRefundRequest.class);
        verify(client).refund(captor.capture());
        assertEquals(28600, captor.getValue().getTotalFee());
        assertEquals(8600, captor.getValue().getRefundFee());
        assertEquals("RF20260919001", captor.getValue().getOutRefundNo());

        // V2 退款受理仅代表 result_code=SUCCESS，终态靠查单/回调 → 非终态 PROCESSING
        assertTrue(result.isAccepted());
        assertEquals(PayStatus.PROCESSING, result.getFinalState());
        assertEquals("RF-ID-1", result.getChannelOrderNo());
    }

    @Test
    void refund_businessFail_returnsNotAccepted() throws WxPayException {
        WxPayOrderQueryResult origin = mock(WxPayOrderQueryResult.class);
        when(origin.getTotalFee()).thenReturn(28600);
        when(origin.getTransactionId()).thenReturn("TX1001");
        when(client.queryOrder("PAY20260919001")).thenReturn(origin);

        WxPayRefundResult refund = mock(WxPayRefundResult.class);
        when(refund.getReturnCode()).thenReturn("SUCCESS");
        when(refund.getResultCode()).thenReturn("FAIL");
        when(refund.getErrCodeDes()).thenReturn("余额不足");
        when(client.refund(any(WxPayRefundRequest.class))).thenReturn(refund);

        RefundResult result = channel.refund(refundCommand());

        assertFalse(result.isAccepted());
        assertEquals(PayStatus.FAIL, result.getFinalState());
    }

    @Test
    void refund_originOrderMissing_throws() throws WxPayException {
        WxPayOrderQueryResult origin = mock(WxPayOrderQueryResult.class);
        when(origin.getTransactionId()).thenReturn(null);
        when(client.queryOrder("PAY20260919001")).thenReturn(origin);

        assertThrows(ServiceException.class, () -> channel.refund(refundCommand()));
    }

    // ==================== queryRefund ====================

    @Test
    void queryRefund_successState() throws WxPayException {
        WxPayRefundQueryResult.RefundRecord record = mock(WxPayRefundQueryResult.RefundRecord.class);
        when(record.getOutRefundNo()).thenReturn("RF20260919001");
        when(record.getRefundStatus()).thenReturn("SUCCESS");
        when(record.getRefundId()).thenReturn("RF-ID-1");

        WxPayRefundQueryResult query = mock(WxPayRefundQueryResult.class);
        when(query.getRefundRecords()).thenReturn(List.of(record));
        when(client.queryRefund("RF20260919001")).thenReturn(query);

        RefundResult result = channel.queryRefund("RF20260919001");

        assertTrue(result.isAccepted());
        assertEquals(PayStatus.SUCCESS, result.getFinalState());
        assertEquals("RF-ID-1", result.getChannelOrderNo());
    }
}
