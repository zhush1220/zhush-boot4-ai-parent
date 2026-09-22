package com.zhush.agentscope.event;

import java.util.function.Supplier;

/**
 * 链路追踪 ID 采集扩展点。
 *
 * <p>事件改为异步分发后，监听器所在线程已不含调用方的链路上下文，因此 {@link AgentEventPublisher}
 * 在<b>发布线程</b>上采集 traceId 并写入事件，供落库监听器（审计日志等）读取。
 *
 * <p>框架层不耦合具体链路实现，由业务模块注册 Bean 提供（如 SkyWalking {@code TraceContext.traceId()}）；
 * 未注册时事件 traceId 为空。
 *
 * @author zhush
 */
@FunctionalInterface
public interface AgentTraceIdSupplier extends Supplier<String> {

    /**
     * 当前链路 ID（无链路上下文时返回 null 或空串）。
     */
    @Override
    String get();
}
