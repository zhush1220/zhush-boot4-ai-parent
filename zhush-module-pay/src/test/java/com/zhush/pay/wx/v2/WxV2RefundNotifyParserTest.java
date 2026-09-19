package com.zhush.pay.wx.v2;

import com.github.binarywang.wxpay.bean.notify.WxPayRefundNotifyResult;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 微信 V2 退款回调解析器契约测试（req_info 解密后归一化）
 */
@ExtendWith(MockitoExtension.class)
class WxV2RefundNotifyParserTest {

    @Mock
    private WxPayService wxPayService;
    @Mock
    private WxPayProperties properties;
    @Mock
    private WxPayProperties.V2 v2;

    private WxV2RefundNotifyParser parser() {
        return new WxV2RefundNotifyParser(wxPayService, properties);
    }

    @Test
    void channelTypeAndNotifyType_areRefund() {
        WxV2RefundNotifyParser parser = parser();
        assertEquals(PayChannelType.WECHAT_V2, parser.channelType());
        assertEquals(PayNotifyType.REFUND, parser.notifyType());
    }

    @Test
    void parse_successRefund_normalizesMessage() throws WxPayException {
        when(properties.getV2()).thenReturn(v2);
        when(v2.getMchId()).thenReturn("MCH001");

        WxPayRefundNotifyResult.ReqInfo reqInfo = mock(WxPayRefundNotifyResult.ReqInfo.class);
        when(reqInfo.getOutRefundNo()).thenReturn("RF20260919001");
        when(reqInfo.getRefundId()).thenReturn("RF-ID-1");
        when(reqInfo.getRefundStatus()).thenReturn("SUCCESS");
        when(reqInfo.getRefundFee()).thenReturn(8600);
        when(reqInfo.getSuccessTime()).thenReturn("20260919123000");

        WxPayRefundNotifyResult notify = mock(WxPayRefundNotifyResult.class);
        when(notify.getMchId()).thenReturn("MCH001");
        when(notify.getReqInfo()).thenReturn(reqInfo);
        when(wxPayService.parseRefundNotifyResult("<xml/>")).thenReturn(notify);

        PayNotifyMessage message = parser().parse("<xml/>", Map.of());

        assertEquals(PayNotifyType.REFUND, message.getNotifyType());
        assertEquals("RF20260919001", message.getBizNo());
        assertEquals("RF-ID-1", message.getChannelOrderNo());
        assertEquals(PayStatus.SUCCESS, message.getPayStatus());
        assertEquals(8600, message.getAmount());
    }

    @Test
    void parse_changeStatus_mapsToProcessing() throws WxPayException {
        when(properties.getV2()).thenReturn(v2);
        when(v2.getMchId()).thenReturn("MCH001");

        WxPayRefundNotifyResult.ReqInfo reqInfo = mock(WxPayRefundNotifyResult.ReqInfo.class);
        when(reqInfo.getRefundStatus()).thenReturn("CHANGE");

        WxPayRefundNotifyResult notify = mock(WxPayRefundNotifyResult.class);
        when(notify.getMchId()).thenReturn("MCH001");
        when(notify.getReqInfo()).thenReturn(reqInfo);
        when(wxPayService.parseRefundNotifyResult("<xml/>")).thenReturn(notify);

        PayNotifyMessage message = parser().parse("<xml/>", Map.of());

        // CHANGE（退款到零钱异常，需人工处理）保守映射为非终态 PROCESSING，交对账收敛
        assertEquals(PayStatus.PROCESSING, message.getPayStatus());
    }

    @Test
    void parse_decryptFail_throws() throws WxPayException {
        when(wxPayService.parseRefundNotifyResult("<xml/>")).thenThrow(new WxPayException("解密失败"));
        assertThrows(ServiceException.class, () -> parser().parse("<xml/>", Map.of()));
    }

    @Test
    void parse_blankBody_throws() {
        assertThrows(ServiceException.class, () -> parser().parse("", Map.of()));
    }
}
