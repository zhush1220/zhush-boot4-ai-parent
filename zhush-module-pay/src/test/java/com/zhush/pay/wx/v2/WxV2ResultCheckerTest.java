package com.zhush.pay.wx.v2;

import com.github.binarywang.wxpay.bean.result.WxPayRefundResult;
import com.zhush.pay.core.model.PayStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 微信 V2 结果码判定测试（return_code / result_code 双层矩阵）
 */
class WxV2ResultCheckerTest {

    private WxPayRefundResult result(String returnCode, String resultCode) {
        WxPayRefundResult r = mock(WxPayRefundResult.class);
        when(r.getReturnCode()).thenReturn(returnCode);
        when(r.getResultCode()).thenReturn(resultCode);
        return r;
    }

    @Test
    void communicateSuccess_onlyReturnCode() {
        assertTrue(WxV2ResultChecker.communicateSuccess(result("SUCCESS", "FAIL")));
        assertFalse(WxV2ResultChecker.communicateSuccess(result("FAIL", "SUCCESS")));
    }

    @Test
    void businessSuccess_requiresBothSuccess() {
        assertTrue(WxV2ResultChecker.businessSuccess(result("SUCCESS", "SUCCESS")));
        assertFalse(WxV2ResultChecker.businessSuccess(result("SUCCESS", "FAIL")));
        assertFalse(WxV2ResultChecker.businessSuccess(result("FAIL", "SUCCESS")));
    }

    @Test
    void toAcceptedState_businessSuccessIsAcceptedNotFinal() {
        // 关键红线：业务成功 → ACCEPTED（受理，非终态），绝不映射为 SUCCESS
        assertEquals(PayStatus.ACCEPTED, WxV2ResultChecker.toAcceptedState(result("SUCCESS", "SUCCESS")));
        assertEquals(PayStatus.FAIL, WxV2ResultChecker.toAcceptedState(result("SUCCESS", "FAIL")));
    }

    @Test
    void nullResult_notCommunicateSuccess() {
        assertFalse(WxV2ResultChecker.communicateSuccess(null));
        assertFalse(WxV2ResultChecker.businessSuccess(null));
    }
}
