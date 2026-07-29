package com.zhush.oauth2.core;

import cn.dev33.satoken.oauth2.data.model.loader.SaClientModel;
import cn.dev33.satoken.oauth2.exception.SaOAuth2ClientModelException;
import cn.dev33.satoken.oauth2.template.SaOAuth2Template;
import cn.hutool.crypto.digest.BCrypt;

/**
 * 支持 BCrypt 密钥比对的 OAuth2 模板
 *
 * <p>sa-token-oauth2 默认按明文 {@code equals} 校验 client_secret，与本项目
 * “client_secret BCrypt 落库、仅创建时回显一次”的安全策略冲突，故覆写比对逻辑：
 * 库中密钥为 BCrypt 哈希（{@code $2a$/$2b$/$2y$} 前缀）时走 {@link BCrypt#checkpw}，
 * 否则回退明文比对（兼容 yml 内联注册的测试客户端）。
 *
 * @author zhush
 */
public class BCryptSecretOAuth2Template extends SaOAuth2Template {

    /**
     * 无效 client_secret 错误码（对齐 sa-token-oauth2 内置值）
     */
    private static final int CODE_INVALID_SECRET = 30115;

    @Override
    public SaClientModel checkClientSecret(String clientId, String clientSecret) {
        SaClientModel clientModel = checkClientModel(clientId);
        if (!secretMatches(clientSecret, clientModel.getClientSecret())) {
            throw new SaOAuth2ClientModelException("无效 client_secret")
                    .setClientId(clientId)
                    .setCode(CODE_INVALID_SECRET);
        }
        return clientModel;
    }

    private boolean secretMatches(String rawSecret, String storedSecret) {
        if (rawSecret == null || storedSecret == null) {
            return false;
        }
        if (storedSecret.startsWith("$2a$") || storedSecret.startsWith("$2b$") || storedSecret.startsWith("$2y$")) {
            return BCrypt.checkpw(rawSecret, storedSecret);
        }
        return storedSecret.equals(rawSecret);
    }

}
