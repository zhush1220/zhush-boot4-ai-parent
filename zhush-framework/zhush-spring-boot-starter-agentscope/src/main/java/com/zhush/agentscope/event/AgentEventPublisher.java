package com.zhush.agentscope.event;

import com.zhush.agentscope.config.AgentScopeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

/**
 * 事件发布器：收集所有 {@link AgentEventListener}，统一分发会话事件。
 *
 * <p><b>默认异步分发</b>：监听器多为落库动作（审计、用量聚合），若同步执行会阻塞 SSE 尾包推送。
 * 线程池由本类自持（关闭时优雅停止），配置见 {@code zhush.agentscope.event-*}；
 * {@code event-async=false} 时退化为同步分发（轻量部署或需严格同会话有序的场景）。
 *
 * <p>线程池饱和被拒绝时，本次事件回退为同步分发，保证审计/用量不丢。
 * 监听器异常一律吞掉并告警，不阻断其它监听者，也不影响主流程。
 *
 * <p>{@code traceId} 在<b>发布线程</b>上采集并写入事件，避免异步线程丢失链路上下文
 * （采集逻辑由 {@link AgentTraceIdSupplier} 提供，框架本身不耦合链路追踪实现）。
 *
 * <p>注意：异步分发下同一会话的不同事件可能乱序，监听器之间不得依赖事件投递顺序。
 *
 * @author zhush
 */
public class AgentEventPublisher implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(AgentEventPublisher.class);

    private final List<AgentEventListener> listeners;

    /**
     * 异步分发线程池，null 表示同步分发
     */
    private final ThreadPoolTaskExecutor executor;

    /**
     * 当前链路 ID 采集器，null 表示不回填
     */
    private final Supplier<String> traceIdSupplier;

    public AgentEventPublisher(List<AgentEventListener> listeners) {
        this.listeners = listeners == null ? List.of() : listeners;
        this.executor = null;
        this.traceIdSupplier = null;
    }

    public AgentEventPublisher(List<AgentEventListener> listeners, AgentScopeProperties properties,
                               AgentTraceIdSupplier traceIdSupplier) {
        this.listeners = listeners == null ? List.of() : listeners;
        this.traceIdSupplier = traceIdSupplier;
        this.executor = properties != null && properties.isEventAsync()
                ? buildExecutor(properties) : null;
    }

    /**
     * 显式指定线程池的构造（测试或外部托管线程池场景），executor 为 null 时同步分发。
     */
    public AgentEventPublisher(List<AgentEventListener> listeners, ThreadPoolTaskExecutor executor,
                               Supplier<String> traceIdSupplier) {
        this.listeners = listeners == null ? List.of() : listeners;
        this.executor = executor;
        this.traceIdSupplier = traceIdSupplier;
    }

    /**
     * 发布事件给所有监听者（单个监听者异常不阻断其它监听者与主流程）。
     *
     * @param event 会话事件
     */
    public void publish(AgentEvent event) {
        if (event == null) {
            return;
        }
        fillTraceId(event);
        if (executor == null) {
            dispatch(event);
            return;
        }
        try {
            executor.execute(() -> dispatch(event));
        } catch (RejectedExecutionException e) {
            // 队列饱和：退化为同步分发，宁可慢一点也不丢审计/用量
            log.warn("Agent 事件异步队列饱和, 退化为同步分发, eventType={}", event.getEventType());
            dispatch(event);
        } catch (Exception e) {
            log.warn("Agent 事件异步分发异常, eventType={}, error={}", event.getEventType(), e.getMessage());
        }
    }

    /**
     * 是否存在监听者。
     *
     * @return 有监听者返回 true
     */
    public boolean hasListeners() {
        return listeners != null && !listeners.isEmpty();
    }

    /**
     * 是否异步分发。
     */
    public boolean isAsync() {
        return executor != null;
    }

    @Override
    public void close() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    private static ThreadPoolTaskExecutor buildExecutor(AgentScopeProperties properties) {
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        int size = Math.max(properties.getEventPoolSize(), 1);
        pool.setCorePoolSize(size);
        pool.setMaxPoolSize(size);
        pool.setQueueCapacity(Math.max(properties.getEventQueueCapacity(), 16));
        pool.setThreadNamePrefix("agent-event-");
        // 队列满时抛 RejectedExecutionException，由 publish 退化为同步分发（AbortPolicy 默认）
        pool.setWaitForTasksToCompleteOnShutdown(true);
        pool.setAwaitTerminationSeconds(10);
        pool.initialize();
        return pool;
    }

    private void dispatch(AgentEvent event) {
        for (AgentEventListener listener : listeners) {
            if (listener == null) {
                continue;
            }
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                log.warn("Agent 事件监听器执行异常, eventType={}, listener={}, error={}",
                        event.getEventType(), listener.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    private void fillTraceId(AgentEvent event) {
        if (traceIdSupplier == null || event.getTraceId() != null) {
            return;
        }
        try {
            event.setTraceId(traceIdSupplier.get());
        } catch (Exception e) {
            log.debug("Agent 事件链路 ID 采集失败: {}", e.getMessage());
        }
    }
}
