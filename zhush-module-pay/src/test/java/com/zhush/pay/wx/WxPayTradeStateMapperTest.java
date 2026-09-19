package com.zhush.pay.wx;

import com.zhush.pay.core.model.PayStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 微信 V2/V3 支付/退款状态映射契约测试
 *
 * <p>资金安全红线：未知/中间态必须保守映射为 {@link PayStatus#PROCESSING}，绝不误判终态。
 */
class WxPayTradeStateMapperTest {

    @Test
    void toPayStatus_mapsKnownStates() {
        assertEquals(PayStatus.SUCCESS, WxPayTradeStateMapper.toPayStatus("SUCCESS"));
        // 已收款后退款：对支付本身仍是成功终态
        assertEquals(PayStatus.SUCCESS, WxPayTradeStateMapper.toPayStatus("REFUND"));
        assertEquals(PayStatus.FAIL, WxPayTradeStateMapper.toPayStatus("CLOSED"));
        assertEquals(PayStatus.FAIL, WxPayTradeStateMapper.toPayStatus("REVOKED"));
        assertEquals(PayStatus.FAIL, WxPayTradeStateMapper.toPayStatus("PAYERROR"));
    }

    @Test
    void toPayStatus_unknownOrNullStaysProcessing() {
        assertEquals(PayStatus.PROCESSING, WxPayTradeStateMapper.toPayStatus("NOTPAY"));
        assertEquals(PayStatus.PROCESSING, WxPayTradeStateMapper.toPayStatus("USERPAYING"));
        assertEquals(PayStatus.PROCESSING, WxPayTradeStateMapper.toPayStatus("SOMETHING_NEW"));
        assertEquals(PayStatus.PROCESSING, WxPayTradeStateMapper.toPayStatus(null));
    }

    @Test
    void toPayStatus_isCaseInsensitive() {
        assertEquals(PayStatus.SUCCESS, WxPayTradeStateMapper.toPayStatus("success"));
    }

    @Test
    void toRefundStatus_mapsKnownStates() {
        assertEquals(PayStatus.SUCCESS, WxPayTradeStateMapper.toRefundStatus("SUCCESS"));
        assertEquals(PayStatus.FAIL, WxPayTradeStateMapper.toRefundStatus("CLOSED"));
        assertEquals(PayStatus.FAIL, WxPayTradeStateMapper.toRefundStatus("ABNORMAL"));
        // V2 特有
        assertEquals(PayStatus.FAIL, WxPayTradeStateMapper.toRefundStatus("REFUNDCLOSE"));
    }

    @Test
    void toRefundStatus_processingAndUnknownStayNonFinal() {
        assertEquals(PayStatus.PROCESSING, WxPayTradeStateMapper.toRefundStatus("PROCESSING"));
        // V2 CHANGE（退款到零钱异常）需人工介入，保守非终态
        assertEquals(PayStatus.PROCESSING, WxPayTradeStateMapper.toRefundStatus("CHANGE"));
        assertEquals(PayStatus.PROCESSING, WxPayTradeStateMapper.toRefundStatus("WEIRD"));
        assertEquals(PayStatus.PROCESSING, WxPayTradeStateMapper.toRefundStatus(null));
    }

}
