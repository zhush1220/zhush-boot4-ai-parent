package com.zhush.pay.wx.v3;

import com.github.binarywang.wxpay.bean.notify.SignatureHeader;
import com.github.binarywang.wxpay.bean.notify.WxPayNotifyV3Result;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.zhush.common.exception.ServiceException;
import com.zhush.pay.core.model.PayChannelType;
import com.zhush.pay.core.model.PayStatus;
import com.zhush.pay.core.notify.PayNotifyMessage;
import com.zhush.pay.core.notify.PayNotifyType;
import com.zhush.pay.wx.WxPayProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 微信 V3 支付（收款）回调解析器契约测试
 */
@ExtendWith(MockitoExtension.class)
class WxV3PaymentNotifyParserTest {

    @Mock
    private WxPayService wxPayService;
    @Mock
    private WxPayProperties properties;
    @Mock
    private WxPayProperties.V3 v3;

    private WxV3PaymentNotifyParser parser() {
        return new WxV3PaymentNotifyParser(wxPayService, properties);
    }

    @Test
    void channelTypeAndNotifyType_arePayment() {
        WxV3PaymentNotifyParser parser = parser();
        assertEquals(PayChannelType.WECHAT_V3, parser.channelType());
        assertEquals(PayNotifyType.PAYMENT, parser.notifyType());
    }

    @Test
    void parse_success_normalizesMessage() throws WxPayException {
        when(properties.getV3()).thenReturn(v3);
        when(v3.getMchId()).thenReturn("MCH001");

        WxPayNotifyV3Result.DecryptNotifyResult result = mock(WxPayNotifyV3Result.DecryptNotifyResult.class);
        WxPayNotifyV3Result.Amount amount = mock(WxPayNotifyV3Result.Amount.class);
        when(result.getMchid()).thenReturn("MCH001");
        when(result.getOutTradeNo()).thenReturn("PAY20260919001");
        when(result.getTransactionId()).thenReturn("TX1001");
        when(result.getTradeState()).thenReturn("SUCCESS");
        when(result.getAmount()).thenReturn(amount);
        when(amount.getPayerTotal()).thenReturn(8888);

        WxPayNotifyV3Result notify = mock(WxPayNotifyV3Result.class);
        when(notify.getResult()).thenReturn(result);
        when(wxPayService.parseOrderNotifyV3Result(any(), any(SignatureHeader.class))).thenReturn(notify);

        PayNotifyMessage message = parser().parse("{}", Map.of("Wechatpay-Signature", "sig"));

        assertEquals(PayChannelType.WECHAT_V3, message.getChannelType());
        assertEquals(PayNotifyType.PAYMENT, message.getNotifyType());
        assertEquals("PAY20260919001", message.getBizNo());
        assertEquals("TX1001", message.getChannelOrderNo());
        assertEquals(PayStatus.SUCCESS, message.getPayStatus());
        assertEquals(8888, message.getAmount());
        assertNull(message.getFailReason());
    }

    @Test
    void parse_mchIdMismatch_throws() throws WxPayException {
        when(properties.getV3()).thenReturn(v3);
        when(v3.getMchId()).thenReturn("MCH001");

        WxPayNotifyV3Result.DecryptNotifyResult result = mock(WxPayNotifyV3Result.DecryptNotifyResult.class);
        when(result.getMchid()).thenReturn("OTHER-MCH");

        WxPayNotifyV3Result notify = mock(WxPayNotifyV3Result.class);
        when(notify.getResult()).thenReturn(result);
        when(wxPayService.parseOrderNotifyV3Result(any(), any(SignatureHeader.class))).thenReturn(notify);

        assertThrows(ServiceException.class, () -> parser().parse("{}", Map.of()));
    }

    @Test
    void parse_verifyFail_throws() throws WxPayException {
        when(wxPayService.parseOrderNotifyV3Result(any(), any(SignatureHeader.class)))
                .thenThrow(new WxPayException("验签失败"));

        assertThrows(ServiceException.class, () -> parser().parse("{}", Map.of()));
    }

    @Test
    void parse_blankBody_throws() {
        assertThrows(ServiceException.class, () -> parser().parse("", Map.of()));
    }

}
