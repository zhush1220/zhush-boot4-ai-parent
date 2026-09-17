package com.zhush.pay.wx;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信支付配置
 *
 * <p>前缀：{@code zhush.pay.wx}，V2 / V3 双协议独立开关，未启用的协议不装配 Bean。
 *
 * <p><b>凭证纪律</b>：全部经环境变量外置，证书走挂载卷；禁止入库、入代码。
 *
 * @author zhush
 */
@Data
@Component
@ConfigurationProperties(prefix = "zhush.pay.wx")
public class WxPayProperties {

    /** 总开关：false 时不装配任何微信支付 Bean */
    private boolean enabled = false;

    /** 默认商户号（多商户路由预留：未指定时取此值） */
    private String primaryMchId;

    /** 统一回调前缀（如 https://<域名>/api/pay/notify），业务可覆盖 */
    private String notifyUrl;

    /** V3 配置（商家转账等） */
    private V3 v3 = new V3();

    /** V2 配置（企业付款到银行卡等） */
    private V2 v2 = new V2();

    /** 渠道级限额 */
    private Limit limit = new Limit();

    /**
     * 校验指定协议的必需凭证，缺失即抛异常（生产 fail-fast，禁止静默降级）
     */
    public void validateV3() {
        require(v3.getAppId(), "v3.app-id");
        require(v3.getMchId(), "v3.mch-id");
        require(v3.getApiV3Key(), "v3.api-v3-key");
        require(v3.getCertSerialNo(), "v3.cert-serial-no");
        require(v3.getPrivateKeyPath(), "v3.private-key-path");
    }

    /**
     * 校验 V2 协议的必需凭证（企业付款到银行卡），缺失即抛异常（生产 fail-fast）
     */
    public void validateV2() {
        require(v2.getAppId(), "v2.app-id");
        require(v2.getMchId(), "v2.mch-id");
        require(v2.getMchKey(), "v2.mch-key");
        // PKCS12 证书为双向 TLS 与付款接口所必需
        require(v2.getKeyPath(), "v2.key-path");
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("[zhush.pay.wx] 缺少必需配置项：" + name);
        }
    }

    /**
     * V3（JSON + SHA256-RSA + AES-256-GCM）
     */
    @Data
    public static class V3 {

        /** 是否启用 V3 */
        private boolean enabled = false;

        /** 公众账号/小程序 AppID（商家转账必填） */
        private String appId;

        /** 商户号 */
        private String mchId;

        /** APIv3 密钥（回调解密） */
        private String apiV3Key;

        /** 商户证书序列号 */
        private String certSerialNo;

        /** 商户私钥路径（容器内 file:/data/certs/apiclient_key.pem） */
        private String privateKeyPath;

        /** 微信支付公钥 ID（公钥模式） */
        private String publicKeyId;

        /** 微信支付公钥路径 */
        private String publicKeyPath;

        /** 转账场景 ID（商家转账必填，商户平台申请） */
        private String transferSceneId;

        /** 转账备注（用户可见，默认「佣金提现」） */
        private String transferRemark = "佣金提现";

        /** 用户收款感知（转账场景名称） */
        private String userRecvPerception;

        /** 收款授权模式（选填） */
        private String receiptAuthorizationMode;

        /** 转账场景报备信息（商家转账必填） */
        private List<SceneReportInfo> transferSceneReportInfos = new ArrayList<>();

    }

    /**
     * 转账场景报备信息
     */
    @Data
    public static class SceneReportInfo {

        /** 报备类型（如 transfer_scene_report_infos） */
        private String infoType;

        /** 报备内容 */
        private String infoContent;

    }

    /**
     * V2（XML + APIv2 密钥 + PKCS12 双向 TLS）
     */
    @Data
    public static class V2 {

        /** 是否启用 V2 */
        private boolean enabled = false;

        /** AppID */
        private String appId;

        /** 商户号 */
        private String mchId;

        /** APIv2 密钥（签名） */
        private String mchKey;

        /** 企业付款专用密钥（可与 mchKey 同值） */
        private String entPayKey;

        /** PKCS12 商户证书路径（双向 TLS；证书口令由 SDK 以商户号解析） */
        private String keyPath;

        /** 签名类型：HMAC-SHA256 / MD5 */
        private String signType = "HMAC-SHA256";

        /** 是否使用沙箱环境 */
        private boolean useSandboxEnv = false;

    }

    /**
     * 渠道级限额
     */
    @Data
    public static class Limit {

        /** 微信 V3 限额 */
        private AmountRange wechatV3 = new AmountRange();

        /** 微信 V2 限额 */
        private AmountRange wechatV2 = new AmountRange();

    }

    /**
     * 金额区间（单位：分）
     */
    @Data
    public static class AmountRange {

        /** 单笔下限（分） */
        private Integer minAmount;

        /** 单笔上限（分） */
        private Integer maxAmount;

    }

}
