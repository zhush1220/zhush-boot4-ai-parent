package com.zhush.oauth2.core;

/**
 * OAuth2 授权页登录处理器（SPI）
 *
 * <p>用户在统一授权页提交账号密码时（{@code POST /oauth2/doLogin}）由授权服务器回调。
 * 由业务模块实现并注册为 Bean（默认对管理端账号体系开放，实现方内部走
 * {@code StpAdminUtil} 登录），返回值将序列化为响应体。
 *
 * @author zhush
 */
@FunctionalInterface
public interface OAuth2DoLoginHandler {

    /**
     * 处理授权页登录
     *
     * @param username 账号
     * @param password 密码
     * @return 登录结果（如 {@code Result.success()}），直接作为 /oauth2/doLogin 响应
     */
    Object doLogin(String username, String password);

}
