package com.zhush.pay.wx.v3;

import com.github.binarywang.wxpay.bean.request.WxPayRefundV3Request;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderV3Request;
import com.github.binarywang.wxpay.bean.result.WxPayOrderQueryV3Result;
import com.github.binarywang.wxpay.bean.result.WxPayRefundQueryV3Result;
import com.github.binarywang.wxpay.bean.result.WxPayRefundV3Result;
import com.github.binarywang.wxpay.bean.result.WxPayUnifiedOrderV3Result;
import com.github.binarywang.wxpay.bean.result.enums.TradeTypeEnum;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.zhush.pay.wx.WxPayProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * 微信 V3 收款/退款客户端（JSAPI 下单 + 查单 + 退款）
 *
 * <p>仅做协议调用与异常透出，不含业务状态机；JSAPI 下单由 SDK 内部用商户私钥完成签名，
 * 返回可直接交前端拉起支付的参数包。
 *
 * @author zhush
 */
@Slf4j
public class WxPaymentClient {

    private final WxPayService wxPayService;
    private final WxPayProperties properties;

    public WxPaymentClient(WxPayService wxPayService, WxPayProperties properties) {
        this.wxPayService = wxPayService;
        this.properties = properties;
    }

    /**
     * JSAPI 下单：返回带 {@code prepay_id} 与支付签名（paySign 等）的参数包
     */
    public WxPayUnifiedOrderV3Result.JsapiResult createJsapiOrder(WxPayUnifiedOrderV3Request request)
            throws WxPayException {
        return wxPayService.createOrderV3(TradeTypeEnum.JSAPI, request);
    }

    /**
     * 按商户订单号查单
     */
    public WxPayOrderQueryV3Result queryOrder(String outTradeNo) throws WxPayException {
        return wxPayService.queryOrderV3(null, outTradeNo);
    }

    /**
     * 发起退款
     */
    public WxPayRefundV3Result refund(WxPayRefundV3Request request) throws WxPayException {
        return wxPayService.refundV3(request);
    }

    /**
     * 按商户退款单号查退款
     */
    public WxPayRefundQueryV3Result queryRefund(String outRefundNo) throws WxPayException {
        return wxPayService.refundQueryV3(outRefundNo);
    }

    /**
     * 默认 AppID
     */
    public String defaultAppId() {
        return properties.getV3().getAppId();
    }

    /**
     * 默认商户号
     */
    public String defaultMchId() {
        return properties.getV3().getMchId();
    }

}
