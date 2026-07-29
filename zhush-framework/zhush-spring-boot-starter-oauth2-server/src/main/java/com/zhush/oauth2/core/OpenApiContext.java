package com.zhush.oauth2.core;

import java.util.List;

/**
 * 开放 API 调用上下文（ThreadLocal）
 *
 * <p>由 {@code OpenApiInterceptor} 鉴权通过后写入，Controller/Service 中可随时读取
 * 当前调用方的 clientId、scope 与用户上下文；请求结束由拦截器统一清理。
 *
 * @author zhush
 */
public final class OpenApiContext {

    private OpenApiContext() {
    }

    private static final ThreadLocal<Caller> HOLDER = new ThreadLocal<>();

    /**
     * 调用方信息
     *
     * @param clientId  客户端标识
     * @param loginId   授权用户 id（用户级调用），应用级调用为 {@code null}
     * @param scopes    令牌持有的 scope 列表
     * @param userLevel 是否用户级调用（access_token=true / client_token=false）
     */
    public record Caller(String clientId, Long loginId, List<String> scopes, boolean userLevel) {
    }

    public static void set(Caller caller) {
        HOLDER.set(caller);
    }

    public static Caller get() {
        return HOLDER.get();
    }

    /**
     * 当前调用方 clientId（未在开放 API 上下文中返回 {@code null}）
     */
    public static String getClientId() {
        Caller caller = HOLDER.get();
        return caller == null ? null : caller.clientId();
    }

    /**
     * 当前授权用户 id（应用级调用或非开放 API 上下文返回 {@code null}）
     */
    public static Long getLoginId() {
        Caller caller = HOLDER.get();
        return caller == null ? null : caller.loginId();
    }

    public static void clear() {
        HOLDER.remove();
    }

}
