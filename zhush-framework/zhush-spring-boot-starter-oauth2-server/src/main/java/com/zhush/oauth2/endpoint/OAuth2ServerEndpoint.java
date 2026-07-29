package com.zhush.oauth2.endpoint;

import cn.dev33.satoken.oauth2.processor.SaOAuth2ServerProcessor;
import com.zhush.satoken.annotation.IgnoreAuth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OAuth2 协议端点（统一分发）
 *
 * <p>由 sa-token-oauth2 处理器按路由分发：
 * {@code /oauth2/authorize}（授权入口）、{@code /oauth2/token}（授权码换令牌）、
 * {@code /oauth2/refresh}（刷新令牌）、{@code /oauth2/revoke}（撤销令牌）、
 * {@code /oauth2/doLogin}（授权页登录）、{@code /oauth2/doConfirm}（确认授权）、
 * {@code /oauth2/client_token}（客户端凭证模式）。
 *
 * <p>协议端点自行完成校验，不走登录态拦截器（{@link IgnoreAuth} + 白名单双保险）。
 *
 * @author zhush
 */
@IgnoreAuth
@RestController
@Tag(name = "OAuth2 协议端点")
public class OAuth2ServerEndpoint {

    @RequestMapping("/oauth2/*")
    @Operation(summary = "OAuth2 协议统一分发（authorize/token/refresh/revoke/doLogin/doConfirm）")
    public Object request() {
        return SaOAuth2ServerProcessor.instance.dister();
    }

}
