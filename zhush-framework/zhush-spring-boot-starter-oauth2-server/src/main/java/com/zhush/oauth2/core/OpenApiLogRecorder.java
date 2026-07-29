package com.zhush.oauth2.core;

/**
 * 开放 API 调用日志记录器（SPI）
 *
 * <p>由业务模块实现并注册为 Bean（参照 {@code OperateLogService} 的异步落库模式），
 * 未提供实现时拦截器跳过日志记录。实现方须自行保证异步与不抛异常。
 *
 * @author zhush
 */
@FunctionalInterface
public interface OpenApiLogRecorder {

    /**
     * 记录一次开放 API 调用（拦截器在请求完成后回调）
     */
    void record(OpenApiCallRecord record);

}
