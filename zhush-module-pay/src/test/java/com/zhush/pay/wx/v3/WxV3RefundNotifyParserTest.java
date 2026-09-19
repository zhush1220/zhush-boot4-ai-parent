package com.zhush.pay.wx.v3;

import com.github.binarywang.wxpay.bean.notify.SignatureHeader;
import com.github.binarywang.wxpay.bean.notify.WxPayRefundNotifyV3Result;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 微信 V3 退款回调解析器契约测试
 */
@ExtendWith(MockitoExtension.class)
class WxV3RefundNotifyParserTest {

    @Mock
    private WxPayService wxPayService;
    @Mock
    private WxPayProperties properties;
    @Mock
    private WxPayProperties.V3 v3;

    private WxV3RefundNotifyParser parser() {
        return new WxV3RefundNotifyParser(wxPayService, properties);
    }

    @Test
    void channelTypeAndNotifyType_areRefund() {
        WxV3RefundNotifyParser parser = parser();
        assertEquals(PayChannelType.WECHAT_V3, parser.channelType());
        assertEquals(PayNotifyType.REFUND, parser.notifyType());
    }

    @Test
    void parse_success_normalizesMessage() throws WxPayException {
        when(properties.getV3()).thenReturn(v3);
        when(v3.getMchId()).thenReturn("MCH001");

        WxPayRefundNotifyV3Result.DecryptNotifyResult result =
                mock(WxPayRefundNotifyV3Result.DecryptNotifyResult.class);
        when(result.getMchid()).thenReturn("MCH001");
        when(result.getOutRefundNo()).thenReturn("RF20260919001");
        when(result.getRefundId()).thenReturn("RF-ID-1");
        when(result.getRefundStatus()).thenReturn("SUCCESS");

        WxPayRefundNotifyV3Result notify = mock(WxPayRefundNotifyV3Result.class);
        when(notify.getResult()).thenReturn(result);
        when(wxPayService.parseRefundNotifyV3Result(any(), any(SignatureHeader.class))).thenReturn(notify);

        PayNotifyMessage message = parser().parse("{}", Map.of());

        assertEquals(PayChannelType.WECHAT_V3, message.getChannelType());
        assertEquals(PayNotifyType.REFUND, message.getNotifyType());
        // 退款回调以商户退款单号为幂等键
        assertEquals("RF20260919001", message.getBizNo());
        assertEquals("RF-ID-1", message.getChannelOrderNo());
        assertEquals(PayStatus.SUCCESS, message.getPayStatus());
    }

    @Test
    void parse_verifyFail_throws() throws WxPayException {
        when(wxPayService.parseRefundNotifyV3Result(any(), any(SignatureHeader.class)))
                .thenThrow(new WxPayException("验签失败"));

        assertThrows(ServiceException.class, () -> parser().parse("{}", Map.of()));
    }

    @Test
    void parse_blankBody_throws() {
        assertThrows(ServiceException.class, () -> parser().parse("", Map.of()));
    }

}
