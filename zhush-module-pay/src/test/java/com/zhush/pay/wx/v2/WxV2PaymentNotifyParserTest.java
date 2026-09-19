package com.zhush.pay.wx.v2;

import com.github.binarywang.wxpay.bean.notify.WxPayOrderNotifyResult;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.zhush.common.exception.ServiceException;
import com.zhush.pay.core.model.PayChannelType;
import com.zhush.pay.core.model.PayStatus;
import com.zhush.pay.core.notify.PayNotifyMessage;
import com.zhush.pay.core.notify.PayNotifyType;
import com.zhush.pay.wx.WxPayProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * 微信 V2 支付回调解析器契约测试
 */
@ExtendWith(MockitoExtension.class)
class WxV2PaymentNotifyParserTest {

    @Mock
    private WxPayService wxPayService;
    @Mock
    private WxPayProperties properties;
    @Mock
    private WxPayProperties.V2 v2;

    private WxV2PaymentNotifyParser parser() {
        return new WxV2PaymentNotifyParser(wxPayService, properties);
    }

    @Test
    void channelTypeAndNotifyType_arePayment() {
        WxV2PaymentNotifyParser parser = parser();
        assertEquals(PayChannelType.WECHAT_V2, parser.channelType());
        assertEquals(PayNotifyType.PAYMENT, parser.notifyType());
    }

    @Test
    void parse_success_normalizesMessage() throws WxPayException {
        when(properties.getV2()).thenReturn(v2);
        when(v2.getMchId()).thenReturn("MCH001");

        WxPayOrderNotifyResult result = new WxPayOrderNotifyResult();
        result.setMchId("MCH001");
        result.setOutTradeNo("PAY20260919001");
        result.setTransactionId("TX1001");
        result.setTotalFee(28600);
        result.setTimeEnd("20260919120000");
        when(wxPayService.parseOrderNotifyResult("<xml/>")).thenReturn(result);

        PayNotifyMessage message = parser().parse("<xml/>", Map.of());

        assertEquals(PayChannelType.WECHAT_V2, message.getChannelType());
        assertEquals(PayNotifyType.PAYMENT, message.getNotifyType());
        assertEquals("PAY20260919001", message.getBizNo());
        assertEquals("TX1001", message.getChannelOrderNo());
        // V2 成功通知即支付成功终态
        assertEquals(PayStatus.SUCCESS, message.getPayStatus());
        assertEquals(28600, message.getAmount());
        assertNotNull(message.getSuccessTime());
    }

    @Test
    void parse_mchIdMismatch_throws() throws WxPayException {
        when(properties.getV2()).thenReturn(v2);
        when(v2.getMchId()).thenReturn("MCH001");

        WxPayOrderNotifyResult result = new WxPayOrderNotifyResult();
        result.setMchId("OTHER-MCH");
        when(wxPayService.parseOrderNotifyResult("<xml/>")).thenReturn(result);

        assertThrows(ServiceException.class, () -> parser().parse("<xml/>", Map.of()));
    }

    @Test
    void parse_verifyFail_throws() throws WxPayException {
        when(wxPayService.parseOrderNotifyResult("<xml/>")).thenThrow(new WxPayException("验签失败"));
        assertThrows(ServiceException.class, () -> parser().parse("<xml/>", Map.of()));
    }

    @Test
    void parse_blankBody_throws() {
        assertThrows(ServiceException.class, () -> parser().parse("", Map.of()));
    }
}
