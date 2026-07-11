package com.zhush.mybatis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 字段加密配置属性
 * <p>
 * 通过 {@code zhush.encrypt.*} 前缀在 application.yaml 中配置：
 * <pre>{@code
 * zhush:
 *   encrypt:
 *     enabled: true
 *     sm4-key: your-16-char-key!
 * }</pre>
 *
 * @author zhush
 * @since 2026/7/11
 */
@ConfigurationProperties(prefix = "zhush.encrypt")
public class EncryptProperties {

    /**
     * 是否启用字段加解密功能（全局开关）
     * <p>
     * 设为 false 时，所有标记 {@code @EncryptField} 的字段将不进行加解密，直接读写明文。
     * 默认启用。
     */
    private boolean enabled = true;

    /**
     * SM4 加密密钥（16 字节 / 128 位）
     * <p>
     * 如果不配置，将回退到 SmUtils 默认的密钥获取策略
     * （系统属性 > 环境变量 > 硬编码默认值）。
     * 生产环境强烈建议显式配置此项。
     */
    private String sm4Key;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSm4Key() {
        return sm4Key;
    }

    public void setSm4Key(String sm4Key) {
        this.sm4Key = sm4Key;
    }
}
