package com.zhush.pay.wx.exception;

import cn.hutool.core.util.StrUtil;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.zhush.common.exception.ServiceException;
import com.zhush.pay.core.model.PayoutResult;
import com.zhush.pay.enums.PayErrorCode;

import static com.zhush.common.exception.ServiceExceptionUtil.exception;

/**
 * 微信渠道异常 → 统一结果/统一错误码 转换
 *
 * <p>只提取可读原因，<b>禁止</b>把密钥、证书、报文密文写入日志。
 *
 * @author zhush
 */
public final class WxPayExceptionTranslator {

    private WxPayExceptionTranslator() {
    }

    /**
     * 提取可读失败原因（已脱敏）
     */
    public static String describe(WxPayException e) {
        if (StrUtil.isNotBlank(e.getErrCode())) {
            return e.getErrCode() + ":" + StrUtil.blankToDefault(e.getErrCodeDes(), e.getReturnMsg());
        }
        return StrUtil.blankToDefault(e.getCustomErrorMsg(),
                StrUtil.blankToDefault(e.getReturnMsg(), e.getMessage()));
    }

    /**
     * 渠道异常 → 统一出款失败结果（accepted=false，绝不伪装成功）
     */
    public static PayoutResult toFailResult(WxPayException e) {
        return PayoutResult.fail(describe(e));
    }

    /**
     * 渠道异常 → 业务异常
     */
    public static ServiceException toServiceException(WxPayException e) {
        return exception(PayErrorCode.WX_PAY_CALL_FAIL, describe(e));
    }

}
