package com.zhush.pay.wx.v2;

import cn.hutool.core.util.StrUtil;
import com.github.binarywang.wxpay.bean.result.BaseWxPayResult;
import com.zhush.pay.core.model.PayStatus;

/**
 * 微信 V2 结果码判定（return_code / result_code）
 *
 * <p>V2 无统一状态字段，以 {@code return_code}（通信）+ {@code result_code}（业务）双层判定；
 * 资金安全红线：业务成功仅代表「受理」，出款/退款终态仍需查单收敛，<b>绝不</b>据此直接判到账。
 *
 * @author zhush
 */
public final class WxV2ResultChecker {

    private static final String SUCCESS = "SUCCESS";

    private WxV2ResultChecker() {
    }

    /** 通信层是否成功（return_code=SUCCESS） */
    public static boolean communicateSuccess(BaseWxPayResult result) {
        return result != null && SUCCESS.equals(result.getReturnCode());
    }

    /** 通信 + 业务均成功（return_code=SUCCESS 且 result_code=SUCCESS） */
    public static boolean businessSuccess(BaseWxPayResult result) {
        return communicateSuccess(result) && SUCCESS.equals(result.getResultCode());
    }

    /**
     * 出款/退款「受理」态映射：业务成功 → {@link PayStatus#ACCEPTED}（非终态，待查单）；否则失败。
     */
    public static PayStatus toAcceptedState(BaseWxPayResult result) {
        return businessSuccess(result) ? PayStatus.ACCEPTED : PayStatus.FAIL;
    }

    /**
     * 提取可读失败原因（脱敏）：优先 err_code_des，回落 return_msg。
     */
    public static String failReason(BaseWxPayResult result) {
        if (result == null) {
            return "渠道无响应";
        }
        if (StrUtil.isNotBlank(result.getErrCodeDes())) {
            return StrUtil.blankToDefault(result.getErrCode(), "") + ":" + result.getErrCodeDes();
        }
        return StrUtil.blankToDefault(result.getReturnMsg(), "渠道返回失败");
    }

}
