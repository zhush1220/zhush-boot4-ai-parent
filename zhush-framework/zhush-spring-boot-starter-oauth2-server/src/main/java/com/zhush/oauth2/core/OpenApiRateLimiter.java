package com.zhush.oauth2.core;

/**
 * 开放 API 限流器（SPI）
 *
 * <p>由业务侧实现并注册为 Bean（可复用 protection starter + Redis 滑动窗口，
 * 按 {@code sys_oauth2_client} 的 {@code qps_limit}/{@code daily_quota} 判定）；
 * 未提供实现时拦截器跳过限流校验。
 *
 * @author zhush
 */
@FunctionalInterface
public interface OpenApiRateLimiter {

    /**
     * 尝试获取一次调用许可
     *
     * @param clientId 客户端标识
     * @return {@code true} 放行；{@code false} 超限（拦截器返回 429）
     */
    boolean tryAcquire(String clientId);

}
