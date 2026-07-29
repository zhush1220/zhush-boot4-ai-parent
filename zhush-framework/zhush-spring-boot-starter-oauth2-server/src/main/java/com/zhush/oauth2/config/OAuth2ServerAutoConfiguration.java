package com.zhush.oauth2.config;

import cn.dev33.satoken.oauth2.SaOAuth2Manager;
import cn.dev33.satoken.oauth2.config.SaOAuth2ServerConfig;
import cn.dev33.satoken.oauth2.data.loader.SaOAuth2DataLoader;
import cn.dev33.satoken.oauth2.strategy.SaOAuth2Strategy;
import com.zhush.common.exception.GlobalErrorCode;
import com.zhush.common.response.Result;
import com.zhush.oauth2.core.BCryptSecretOAuth2Template;
import com.zhush.oauth2.core.OAuth2DoLoginHandler;
import com.zhush.oauth2.core.OpenApiInterceptor;
import com.zhush.oauth2.core.OpenApiLogRecorder;
import com.zhush.oauth2.core.OpenApiRateLimiter;
import com.zhush.oauth2.endpoint.OAuth2ServerEndpoint;
import com.zhush.satoken.core.StpAdminUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Map;

/**
 * OAuth2 授权服务器自动配置
 *
 * <p>职责：
 * <ul>
 *     <li>装配 sa-token-oauth2 全局配置（授权模式开关、令牌有效期），登录态绑定管理端账号体系（{@link StpAdminUtil}）</li>
 *     <li>桥接业务侧 {@link SaOAuth2DataLoader}（客户端信息从 DB 加载）与 {@link OAuth2DoLoginHandler}（授权页登录）</li>
 *     <li>前后端分离适配：未登录视图 / 授权确认视图返回 JSON，由前端统一登录页与确认页渲染</li>
 *     <li>注册开放 API 鉴权拦截器 {@link OpenApiInterceptor} 到 {@code zhush.oauth2.open-api-prefix}</li>
 * </ul>
 *
 * @author zhush
 */
@AutoConfiguration
@EnableConfigurationProperties(OAuth2ServerProperties.class)
@ConditionalOnProperty(prefix = "zhush.oauth2", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OAuth2ServerAutoConfiguration implements WebMvcConfigurer {

    private final OAuth2ServerProperties properties;
    private final ObjectProvider<SaOAuth2DataLoader> dataLoaderProvider;
    private final ObjectProvider<OAuth2DoLoginHandler> doLoginHandlerProvider;
    private final ObjectProvider<OpenApiLogRecorder> logRecorderProvider;
    private final ObjectProvider<OpenApiRateLimiter> rateLimiterProvider;

    public OAuth2ServerAutoConfiguration(OAuth2ServerProperties properties,
                                         ObjectProvider<SaOAuth2DataLoader> dataLoaderProvider,
                                         ObjectProvider<OAuth2DoLoginHandler> doLoginHandlerProvider,
                                         ObjectProvider<OpenApiLogRecorder> logRecorderProvider,
                                         ObjectProvider<OpenApiRateLimiter> rateLimiterProvider) {
        this.properties = properties;
        this.dataLoaderProvider = dataLoaderProvider;
        this.doLoginHandlerProvider = doLoginHandlerProvider;
        this.logRecorderProvider = logRecorderProvider;
        this.rateLimiterProvider = rateLimiterProvider;
    }

    @PostConstruct
    public void configureOAuth2Server() {
        // 1. 全局服务端配置：开启授权码/客户端凭证，关闭 implicit/password（对齐 OAuth2.1）
        SaOAuth2ServerConfig config = new SaOAuth2ServerConfig();
        config.setEnableAuthorizationCode(true);
        config.setEnableClientCredentials(true);
        config.setEnableImplicit(false);
        config.setEnablePassword(false);
        config.setCodeTimeout(properties.getCodeTimeout());
        config.setAccessTokenTimeout(properties.getAccessTokenTimeout());
        config.setRefreshTokenTimeout(properties.getRefreshTokenTimeout());
        config.setClientTokenTimeout(properties.getClientTokenTimeout());
        SaOAuth2Manager.setServerConfig(config);

        // 2. 客户端密钥改用 BCrypt 比对（库中密钥为哈希，仅创建时回显一次）
        SaOAuth2Manager.setTemplate(new BCryptSecretOAuth2Template());

        // 3. 授权登录态绑定管理端账号体系（会员端开放平台如有需要另行注册实例）
        SaOAuth2Manager.setStpLogic(StpAdminUtil.STP_LOGIC);

        // 4. 客户端数据加载器由业务模块（zhush-module-system）提供实现
        dataLoaderProvider.ifAvailable(SaOAuth2Manager::setDataLoader);

        // 5. 前后端分离视图适配：返回 JSON，由前端渲染统一登录页 / 授权确认页
        SaOAuth2Strategy.instance.notLoginView =
                () -> Result.error(GlobalErrorCode.UNAUTHORIZED, "未登录，请先登录后再进行授权");
        SaOAuth2Strategy.instance.confirmView =
                (clientId, scopes) -> Result.success(Map.of("clientId", clientId, "scopes", scopes));
        doLoginHandlerProvider.ifAvailable(handler ->
                SaOAuth2Strategy.instance.doLoginHandle = handler::doLogin);
    }

    @Bean
    public OAuth2ServerEndpoint oauth2ServerEndpoint() {
        return new OAuth2ServerEndpoint();
    }

    @Bean
    public OpenApiInterceptor openApiInterceptor() {
        return new OpenApiInterceptor(logRecorderProvider.getIfAvailable(), rateLimiterProvider.getIfAvailable());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(openApiInterceptor())
                .addPathPatterns(properties.getOpenApiPrefix());
    }

}
