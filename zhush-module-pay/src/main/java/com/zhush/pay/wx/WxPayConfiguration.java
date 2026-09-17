package com.zhush.pay.wx;

import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.service.WxPayService;
import com.github.binarywang.wxpay.service.impl.WxPayServiceImpl;
import com.zhush.pay.wx.v2.WxBankPayClient;
import com.zhush.pay.wx.v2.WxV2PayoutChannel;
import com.zhush.pay.wx.v3.WxTransferClient;
import com.zhush.pay.wx.v3.WxTransferNotifyParser;
import com.zhush.pay.wx.v3.WxV3PayoutChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 微信支付装配
 *
 * <p><b>条件装配</b>：{@code zhush.pay.wx.enabled=true} 才装配 {@link WxPayService}；
 * V2 / V3 各自独立开关，未启用的协议不产生 Bean（也就不会被 {@code PayGateway} 路由到）。
 *
 * <p><b>fail-fast</b>：协议启用时校验必需凭证，缺失则<b>启动即失败</b>，禁止静默降级。
 *
 * @author zhush
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "zhush.pay.wx", name = "enabled", havingValue = "true")
public class WxPayConfiguration {

    /**
     * 微信支付客户端（同一实例同时承载 V2 与 V3 两套凭证）
     */
    @Bean
    public WxPayService wxPayService(WxPayProperties properties) {
        WxPayConfig config = new WxPayConfig();

        WxPayProperties.V3 v3 = properties.getV3();
        if (v3.isEnabled()) {
            properties.validateV3();
            config.setAppId(v3.getAppId());
            config.setMchId(v3.getMchId());
            config.setApiV3Key(v3.getApiV3Key());
            config.setCertSerialNo(v3.getCertSerialNo());
            config.setPrivateKeyPath(v3.getPrivateKeyPath());
            config.setPublicKeyId(v3.getPublicKeyId());
            config.setPublicKeyPath(v3.getPublicKeyPath());
            log.info("[WxPay] V3 已启用, mchId={}, certSerialNo={}", v3.getMchId(), v3.getCertSerialNo());
        }

        WxPayProperties.V2 v2 = properties.getV2();
        if (v2.isEnabled()) {
            properties.validateV2();
            config.setMchKey(v2.getMchKey());
            config.setEntPayKey(v2.getEntPayKey());
            // PKCS12 商户证书：SDK 内部以商户号作为证书口令解析，无需单独配置密码
            config.setKeyPath(v2.getKeyPath());
            config.setSignType(v2.getSignType());
            config.setUseSandboxEnv(v2.isUseSandboxEnv());
            if (config.getAppId() == null) {
                config.setAppId(v2.getAppId());
            }
            if (config.getMchId() == null) {
                config.setMchId(v2.getMchId());
            }
            log.info("[WxPay] V2 已启用, mchId={}", v2.getMchId());
        }

        WxPayService wxPayService = new WxPayServiceImpl();
        wxPayService.setConfig(config);
        return wxPayService;
    }

    /**
     * V2 企业付款到银行卡客户端（pay_bank）
     */
    @Bean
    @ConditionalOnProperty(prefix = "zhush.pay.wx.v2", name = "enabled", havingValue = "true")
    public WxBankPayClient wxBankPayClient(WxPayService wxPayService) {
        return new WxBankPayClient(wxPayService);
    }

    /**
     * V2 出款渠道实现（注册进 PayGateway 路由表）
     */
    @Bean
    @ConditionalOnProperty(prefix = "zhush.pay.wx.v2", name = "enabled", havingValue = "true")
    public WxV2PayoutChannel wxV2PayoutChannel(WxBankPayClient wxBankPayClient, WxPayProperties properties) {
        return new WxV2PayoutChannel(wxBankPayClient, properties);
    }

    /**
     * V3 商家转账客户端（transfer-bills）
     */
    @Bean
    @ConditionalOnProperty(prefix = "zhush.pay.wx.v3", name = "enabled", havingValue = "true")
    public WxTransferClient wxTransferClient(WxPayService wxPayService, WxPayProperties properties) {
        return new WxTransferClient(wxPayService, properties);
    }

    /**
     * V3 出款渠道实现（注册进 PayGateway 路由表）
     */
    @Bean
    @ConditionalOnProperty(prefix = "zhush.pay.wx.v3", name = "enabled", havingValue = "true")
    public WxV3PayoutChannel wxV3PayoutChannel(WxTransferClient wxTransferClient, WxPayProperties properties) {
        return new WxV3PayoutChannel(wxTransferClient, properties);
    }

    /**
     * V3 回调解析器（验签 + 解密 → 统一回调消息）
     */
    @Bean
    @ConditionalOnProperty(prefix = "zhush.pay.wx.v3", name = "enabled", havingValue = "true")
    public WxTransferNotifyParser wxTransferNotifyParser(WxPayService wxPayService, WxPayProperties properties) {
        return new WxTransferNotifyParser(wxPayService, properties);
    }

}
