package com.zhush.agentscope.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话续跑共享通道（路线 A/B 共用，SSE）。
 *
 * <p>审批是「异步、跨请求」的：被挂起/确认的续跑产出无法推回早已断开的原始 HTTP 连接，
 * 故为每个会话维护一条独立的 {@link Sinks.Many} 通道，前端经
 * {@code GET /admin/ai/agent/session/{id}/resume-stream} 订阅，即可像首次对话一样流式接收续跑增量。
 *
 * <p><b>限制（务必知晓）</b>：当前为单实例内存实现，通道与订阅者同进程才生效；
 * 多实例水平扩展需替换为 Redis pub/sub 或粘性路由（路线 B 的 {@code AgentState} 已在 Redis，
 * 但事件推送通道仍需外置）。默认不产生任何流量，仅在存在待续跑审批时被业务层写入。
 *
 * @author zhush
 */
public class AgentResumeChannels {

    private static final Logger log = LoggerFactory.getLogger(AgentResumeChannels.class);

    private final Map<String, Sinks.Many<AgentEvent>> channels = new ConcurrentHashMap<>();

    /**
     * 订阅指定会话的续跑事件流（懒建通道）。前端断连后重连可再次调用。
     */
    public Flux<AgentEvent> subscribe(String sessionId) {
        Sinks.Many<AgentEvent> sink = channels.computeIfAbsent(sessionId,
                id -> Sinks.many().multicast().onBackpressureBuffer());
        return sink.asFlux();
    }

    /**
     * 向会话通道推送一条事件（无订阅者时静默丢弃，续跑结果已另行落库供轮询）。
     */
    public void emit(String sessionId, AgentEvent event) {
        if (sessionId == null || event == null) {
            return;
        }
        Sinks.Many<AgentEvent> sink = channels.get(sessionId);
        if (sink != null) {
            Sinks.EmitResult result = sink.tryEmitNext(event);
            if (result.isFailure()) {
                log.debug("续跑通道推送失败, sessionId={}, result={}", sessionId, result);
            }
        }
    }

    /**
     * 结束并回收会话通道（续跑完成或失败后调用，避免 Sink 泄漏）。
     */
    public void complete(String sessionId) {
        if (sessionId == null) {
            return;
        }
        Sinks.Many<AgentEvent> sink = channels.remove(sessionId);
        if (sink != null) {
            sink.tryEmitComplete();
        }
    }
}
