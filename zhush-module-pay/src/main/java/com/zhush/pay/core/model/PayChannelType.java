package com.zhush.pay.core.model;

import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付渠道类型（协议维度）
 *
 * <p>与业务侧的「提现方式」（银行卡/支付宝/微信）做<b>显式映射</b>，不混用：
 * 同一「微信支付」业务方式可落到 {@link #WECHAT_V2}（企业付款到银行卡）或
 * {@link #WECHAT_V3}（商家转账）两种协议，由业务字段 {@code pay_channel} 表达。
 *
 * @author zhush
 */
@Getter
@AllArgsConstructor
public enum PayChannelType {

    /** 微信支付 V2（XML + APIv2 密钥 + PKCS12 双向 TLS） */
    WECHAT_V2("WECHAT_V2", "微信支付V2"),

    /** 微信支付 V3（JSON + SHA256-RSA + AES-256-GCM） */
    WECHAT_V3("WECHAT_V3", "微信支付V3"),

    /** 支付宝（预留） */
    ALIPAY("ALIPAY", "支付宝"),

    ;

    private final String code;
    private final String description;

    /**
     * 按渠道编码解析，未匹配返回 {@link Optional#empty()}
     */
    public static Optional<PayChannelType> of(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(t -> t.code.equalsIgnoreCase(code))
                .findFirst();
    }

}
