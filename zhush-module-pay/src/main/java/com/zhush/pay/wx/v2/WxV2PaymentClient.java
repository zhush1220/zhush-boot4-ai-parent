package com.zhush.pay.wx.v2;

import com.github.binarywang.wxpay.bean.order.WxPayMpOrderResult;
import com.github.binarywang.wxpay.bean.request.WxPayOrderQueryRequest;
import com.github.binarywang.wxpay.bean.request.WxPayRefundQueryRequest;
import com.github.binarywang.wxpay.bean.request.WxPayRefundRequest;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderRequest;
import com.github.binarywang.wxpay.bean.result.WxPayOrderQueryResult;
import com.github.binarywang.wxpay.bean.result.WxPayRefundQueryResult;
import com.github.binarywang.wxpay.bean.result.WxPayRefundResult;
import com.github.binarywang.wxpay.constant.WxPayConstants;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.zhush.pay.wx.WxPayProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * 微信 V2 收款/退款客户端（统一下单 + 查单 + 退款 + 退款查询）
 *
 * <p>仅做协议调用与异常透出，不含业务状态机。V2 报文为 XML、签名 MD5/HMAC-SHA256；
 * 退款/查询走 PKCS12 双向 TLS（由 {@code wxPayService} 配置的 keyPath 提供）。
 *
 * @author zhush
 */
@Slf4j
public class WxV2PaymentClient {

    private final WxPayService wxPayService;
    private final WxPayProperties properties;

    public WxV2PaymentClient(WxPayService wxPayService, WxPayProperties properties) {
        this.wxPayService = wxPayService;
        this.properties = properties;
    }

    /**
     * JSAPI 统一下单：SDK 内部完成签名，返回可直接交前端拉起支付的参数包（packageValue=prepay_id=xxx）
     */
    public WxPayMpOrderResult createJsapiOrder(WxPayUnifiedOrderRequest request) throws WxPayException {
        return wxPayService.createOrder(WxPayConstants.TradeType.Specific.JSAPI, request);
    }

    /**
     * 按商户订单号查单
     */
    public WxPayOrderQueryResult queryOrder(String outTradeNo) throws WxPayException {
        WxPayOrderQueryRequest request = new WxPayOrderQueryRequest();
        request.setOutTradeNo(outTradeNo);
        return wxPayService.queryOrder(request);
    }

    /**
     * 发起退款（支持部分退款/退差价：total_fee=原单总额，refund_fee=本次额）
     */
    public WxPayRefundResult refund(WxPayRefundRequest request) throws WxPayException {
        return wxPayService.refund(request);
    }

    /**
     * 按商户退款单号查退款
     */
    public WxPayRefundQueryResult queryRefund(String outRefundNo) throws WxPayException {
        WxPayRefundQueryRequest request = new WxPayRefundQueryRequest();
        request.setOutRefundNo(outRefundNo);
        return wxPayService.refundQuery(request);
    }

    /**
     * 默认 AppID（V2）
     */
    public String defaultAppId() {
        return properties.getV2().getAppId();
    }

    /**
     * 默认商户号（V2）
     */
    public String defaultMchId() {
        return properties.getV2().getMchId();
    }

}
