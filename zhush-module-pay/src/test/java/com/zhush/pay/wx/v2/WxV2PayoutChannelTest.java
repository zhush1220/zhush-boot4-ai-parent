package com.zhush.pay.wx.v2;

import com.github.binarywang.wxpay.bean.entpay.EntPayBankQueryResult;
import com.github.binarywang.wxpay.bean.entpay.EntPayBankRequest;
import com.github.binarywang.wxpay.bean.entpay.EntPayBankResult;
import com.github.binarywang.wxpay.bean.entpay.EntPayQueryResult;
import com.github.binarywang.wxpay.bean.entpay.EntPayRequest;
import com.github.binarywang.wxpay.bean.entpay.EntPayResult;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.zhush.common.exception.ServiceException;
import com.zhush.pay.core.model.PayChannelType;
import com.zhush.pay.core.model.PayStatus;
import com.zhush.pay.core.model.Payee;
import com.zhush.pay.core.model.PayoutCommand;
import com.zhush.pay.core.model.PayoutResult;
import com.zhush.pay.wx.WxPayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 微信 V2 企业付款出款渠道契约测试
 *
 * <p>验证：受理仅 ACCEPTED（非到账）；到银行卡敏感字段预加密；异常不伪装受理；无回调靠查单收敛。
 */
@ExtendWith(MockitoExtension.class)
class WxV2PayoutChannelTest {

    @Mock
    private WxV2PayoutClient client;
    @Mock
    private WxPayProperties properties;
    @Mock
    private WxPayProperties.V2 v2;
    @Mock
    private WxPayProperties.Limit limit;

    private WxV2PayoutChannel channel;

    @BeforeEach
    void setUp() {
        channel = new WxV2PayoutChannel(client, properties);
    }

    private PayoutCommand toBank() {
        return PayoutCommand.builder()
                .bizNo("PO20260919001")
                .amount(10000)
                .channelType(PayChannelType.WECHAT_V2)
                .payee(Payee.builder()
                        .accountNo("6222021234567890123").accountName("张三").bankCode("ICBC").build())
                .remark("提现")
                .build();
    }

    private PayoutCommand toBalance() {
        return PayoutCommand.builder()
                .bizNo("PO20260919002")
                .amount(5000)
                .channelType(PayChannelType.WECHAT_V2)
                .payee(Payee.builder().openid("openid-abc").build())
                .remark("佣金")
                .build();
    }

    // ==================== payout 到银行卡 ====================

    @Test
    void payoutToBank_encryptsAndAcceptsNotFinal() throws WxPayException {
        when(properties.getV2()).thenReturn(v2);
        when(properties.getLimit()).thenReturn(limit);
        when(client.encryptSensitive(anyString())).thenReturn("CIPHER");

        EntPayBankResult result = mock(EntPayBankResult.class);
        when(result.getReturnCode()).thenReturn("SUCCESS");
        when(result.getResultCode()).thenReturn("SUCCESS");
        when(result.getPaymentNo()).thenReturn("PN1");
        when(client.payBank(any(EntPayBankRequest.class))).thenReturn(result);

        PayoutResult payout = channel.payout(toBank());

        ArgumentCaptor<EntPayBankRequest> captor = ArgumentCaptor.forClass(EntPayBankRequest.class);
        verify(client).payBank(captor.capture());
        assertEquals("CIPHER", captor.getValue().getEncBankNo());
        assertEquals("CIPHER", captor.getValue().getEncTrueName());
        assertEquals("ICBC", captor.getValue().getBankCode());

        // 受理非终态：绝不判 SUCCESS
        assertTrue(payout.isAccepted());
        assertEquals(PayStatus.ACCEPTED, payout.getFinalState());
        assertFalse(payout.getFinalState().isFinal());
    }

    @Test
    void payoutToBank_missingBankCode_throws() {
        when(properties.getV2()).thenReturn(v2);
        when(properties.getLimit()).thenReturn(limit);
        PayoutCommand command = toBank();
        command.getPayee().setBankCode(null);

        assertThrows(ServiceException.class, () -> channel.payout(command));
    }

    // ==================== payout 到零钱 ====================

    @Test
    void payoutToBalance_acceptsNotFinal() throws WxPayException {
        when(properties.getV2()).thenReturn(v2);
        when(properties.getLimit()).thenReturn(limit);

        EntPayResult result = mock(EntPayResult.class);
        when(result.getReturnCode()).thenReturn("SUCCESS");
        when(result.getResultCode()).thenReturn("SUCCESS");
        when(result.getRetCode()).thenReturn("SUCCESS");
        when(result.getPaymentNo()).thenReturn("PN2");
        when(client.entPay(any(EntPayRequest.class))).thenReturn(result);

        PayoutResult payout = channel.payout(toBalance());

        assertTrue(payout.isAccepted());
        assertEquals(PayStatus.ACCEPTED, payout.getFinalState());
    }

    @Test
    void payout_channelError_notFakeAccepted() throws WxPayException {
        when(properties.getV2()).thenReturn(v2);
        when(properties.getLimit()).thenReturn(limit);
        when(client.encryptSensitive(anyString())).thenReturn("CIPHER");
        when(client.payBank(any(EntPayBankRequest.class))).thenThrow(new WxPayException("网关异常"));

        PayoutResult payout = channel.payout(toBank());

        assertFalse(payout.isAccepted());
        assertEquals(PayStatus.FAIL, payout.getFinalState());
    }

    // ==================== query（无回调→查单收敛） ====================

    @Test
    void query_bankSuccess() throws WxPayException {
        EntPayBankQueryResult bank = mock(EntPayBankQueryResult.class);
        when(bank.getStatus()).thenReturn("SUCCESS");
        when(bank.getPaymentNo()).thenReturn("PN1");
        when(client.queryPayBank("PO20260919001")).thenReturn(bank);

        PayoutResult result = channel.query("PO20260919001");

        assertTrue(result.isAccepted());
        assertEquals(PayStatus.SUCCESS, result.getFinalState());
    }

    @Test
    void query_fallbackToBalanceWhenBankMisses() throws WxPayException {
        when(client.queryPayBank("PO20260919002")).thenThrow(new WxPayException("NOT_FOUND"));
        EntPayQueryResult balance = mock(EntPayQueryResult.class);
        when(balance.getStatus()).thenReturn("PROCESSING");
        when(client.queryEntPay("PO20260919002")).thenReturn(balance);

        PayoutResult result = channel.query("PO20260919002");

        assertTrue(result.isAccepted());
        // 未知/处理中一律非终态，交后续查单/对账
        assertEquals(PayStatus.PROCESSING, result.getFinalState());
    }

    @Test
    void query_bothFail_returnsUnknownNotFail() throws WxPayException {
        when(client.queryPayBank(anyString())).thenThrow(new WxPayException("err1"));
        when(client.queryEntPay(anyString())).thenThrow(new WxPayException("err2"));

        PayoutResult result = channel.query("PO_X");

        assertFalse(result.isAccepted());
    }

    // ==================== cancel ====================

    @Test
    void supportCancel_isFalse() {
        assertFalse(channel.supportCancel());
        assertThrows(ServiceException.class, () -> channel.cancel("PO20260919001"));
    }

    @Test
    void validate_missingPayee_throws() {
        PayoutCommand command = PayoutCommand.builder()
                .bizNo("PO_X").amount(100).channelType(PayChannelType.WECHAT_V2).build();
        assertThrows(ServiceException.class, () -> channel.payout(command));
    }
}
