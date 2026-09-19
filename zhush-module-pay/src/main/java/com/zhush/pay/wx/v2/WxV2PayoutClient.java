package com.zhush.pay.wx.v2;

import com.github.binarywang.wxpay.bean.entpay.EntPayBankQueryResult;
import com.github.binarywang.wxpay.bean.entpay.EntPayBankRequest;
import com.github.binarywang.wxpay.bean.entpay.EntPayBankResult;
import com.github.binarywang.wxpay.bean.entpay.EntPayQueryResult;
import com.github.binarywang.wxpay.bean.entpay.EntPayRequest;
import com.github.binarywang.wxpay.bean.entpay.EntPayResult;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.zhush.pay.wx.WxPayProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * 微信 V2 企业付款客户端（出款到零钱 / 到银行卡 + 查单 + 敏感字段加密）
 *
 * <p>封装 SDK {@code WxPayService#getEntPayService()} 的能力，仅做协议调用与异常透出，
 * 不含业务状态机。V2 报文为 XML、签名 MD5/HMAC-SHA256；出款走 PKCS12 双向 TLS（keyPath）。
 *
 * <p><b>无回调</b>：V2 企业付款不提供结果异步通知，终态只能靠 {@link #queryEntPay}/{@link #queryPayBank}
 * 主动查单收敛，业务侧须配合定时对账。
 *
 * @author zhush
 */
@Slf4j
public class WxV2PayoutClient {

    private final WxPayService wxPayService;
    private final WxPayProperties properties;

    public WxV2PayoutClient(WxPayService wxPayService, WxPayProperties properties) {
        this.wxPayService = wxPayService;
        this.properties = properties;
    }

    /**
     * 企业付款到微信零钱（openid 收款，无敏感字段加密）
     */
    public EntPayResult entPay(EntPayRequest request) throws WxPayException {
        return wxPayService.getEntPayService().entPay(request);
    }

    /**
     * 企业付款到银行卡（encBankNo/encTrueName 须已用 {@link #encryptSensitive} 预加密）
     */
    public EntPayBankResult payBank(EntPayBankRequest request) throws WxPayException {
        return wxPayService.getEntPayService().payBank(request);
    }

    /**
     * 查单：到零钱（按商户付款单号）
     */
    public EntPayQueryResult queryEntPay(String partnerTradeNo) throws WxPayException {
        return wxPayService.getEntPayService().queryEntPay(partnerTradeNo);
    }

    /**
     * 查单：到银行卡（按商户付款单号）
     */
    public EntPayBankQueryResult queryPayBank(String partnerTradeNo) throws WxPayException {
        return wxPayService.getEntPayService().queryPayBank(partnerTradeNo);
    }

    /**
     * 用微信支付公钥加密银行卡敏感字段（卡号 / 姓名）。
     *
     * <p>公钥即时向网关获取，不落库、不出日志；返回 Base64 密文供 {@code EntPayBankRequest} 使用。
     */
    public String encryptSensitive(String plain) throws WxPayException {
        String publicKey = wxPayService.getEntPayService().getPublicKey();
        return WxV2BankCipher.encrypt(publicKey, plain);
    }

    /**
     * 默认商户号（V2）
     */
    public String defaultMchId() {
        return properties.getV2().getMchId();
    }

    /**
     * 默认 AppID（V2）
     */
    public String defaultAppId() {
        return properties.getV2().getAppId();
    }

}
