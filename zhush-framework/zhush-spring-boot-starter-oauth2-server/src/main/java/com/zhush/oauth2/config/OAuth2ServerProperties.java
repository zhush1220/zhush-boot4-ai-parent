package com.zhush.oauth2.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OAuth2 授权服务器 / 开放 API 配置项
 *
 * <p>前缀：{@code zhush.oauth2}
 *
 * @author zhush
 */
@ConfigurationProperties(prefix = "zhush.oauth2")
public class OAuth2ServerProperties {

    /**
     * 是否启用 OAuth2 授权服务器与开放 API 能力
     */
    private boolean enabled = true;

    /**
     * 授权码有效期（秒），一次性使用
     */
    private long codeTimeout = 5 * 60;

    /**
     * access_token 有效期（秒）
     */
    private long accessTokenTimeout = 2 * 60 * 60;

    /**
     * refresh_token 有效期（秒）
     */
    private long refreshTokenTimeout = 30L * 24 * 60 * 60;

    /**
     * client_token（客户端凭证模式）有效期（秒）
     */
    private long clientTokenTimeout = 2 * 60 * 60;

    /**
     * 开放 API 路径前缀（Ant 风格），由 OpenApiInterceptor 接管鉴权
     */
    private String openApiPrefix = "/open/**";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getCodeTimeout() {
        return codeTimeout;
    }

    public void setCodeTimeout(long codeTimeout) {
        this.codeTimeout = codeTimeout;
    }

    public long getAccessTokenTimeout() {
        return accessTokenTimeout;
    }

    public void setAccessTokenTimeout(long accessTokenTimeout) {
        this.accessTokenTimeout = accessTokenTimeout;
    }

    public long getRefreshTokenTimeout() {
        return refreshTokenTimeout;
    }

    public void setRefreshTokenTimeout(long refreshTokenTimeout) {
        this.refreshTokenTimeout = refreshTokenTimeout;
    }

    public long getClientTokenTimeout() {
        return clientTokenTimeout;
    }

    public void setClientTokenTimeout(long clientTokenTimeout) {
        this.clientTokenTimeout = clientTokenTimeout;
    }

    public String getOpenApiPrefix() {
        return openApiPrefix;
    }

    public void setOpenApiPrefix(String openApiPrefix) {
        this.openApiPrefix = openApiPrefix;
    }

}
