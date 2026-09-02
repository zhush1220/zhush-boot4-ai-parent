package com.zhush.ai.service;

import cn.hutool.crypto.SecureUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * AI 模型 API Key 加密组件：AES 对称加密（密钥来自配置，派生 32 字节）。
 *
 * <p>存储态为密文，仅运行态解密构建模型；列表/详情仅返回掩码。
 * 支持 {@code ${ENV:default}} 占位（不加密，运行时解析环境变量，兼容存量）。
 *
 * @author zhush
 */
@Component
public class AiModelCipher {

    private static final Logger log = LoggerFactory.getLogger(AiModelCipher.class);

    private final byte[] keyBytes;

    public AiModelCipher(@Value("${zhush.agentscope.security.model-encrypt-key:zhush.ai.model.default.key}")
                         String configKey) {
        String hex = SecureUtil.sha256(configKey);
        this.keyBytes = hex.substring(0, 32).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 加密（${...} 占位原样返回）。
     */
    public synchronized String encrypt(String plain) {
        if (plain == null || plain.isEmpty()) {
            return "";
        }
        if (isEnvPlaceholder(plain)) {
            return plain;
        }
        return SecureUtil.aes(keyBytes).encryptBase64(plain);
    }

    /**
     * 解密并解析 ${ENV:default} 占位（解析失败返回原值）。
     */
    public synchronized String resolve(String stored) {
        if (stored == null || stored.isEmpty()) {
            return "";
        }
        if (isEnvPlaceholder(stored)) {
            return resolveEnv(stored);
        }
        try {
            return SecureUtil.aes(keyBytes).decryptStr(stored);
        } catch (Exception e) {
            log.warn("API Key 解密失败（可能为明文存量数据），按原值处理");
            return stored;
        }
    }

    /**
     * 掩码展示（永不返回完整明文）。
     */
    public String mask(String plain) {
        if (plain == null || plain.isEmpty()) {
            return "";
        }
        if (plain.length() <= 8) {
            return "****";
        }
        return plain.substring(0, 4) + "****" + plain.substring(plain.length() - 4);
    }

    private boolean isEnvPlaceholder(String value) {
        return value.startsWith("${") && value.endsWith("}");
    }

    private String resolveEnv(String placeholder) {
        String inner = placeholder.substring(2, placeholder.length() - 1);
        String envName = inner;
        String defaultValue = "";
        int colon = inner.indexOf(':');
        if (colon >= 0) {
            envName = inner.substring(0, colon);
            defaultValue = inner.substring(colon + 1);
        }
        String value = System.getenv(envName);
        return value == null || value.isEmpty() ? defaultValue : value;
    }
}
