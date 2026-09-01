package com.zhush.agentscope.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * 事件发布器：收集所有 {@link AgentEventListener}，统一分发会话事件。
 *
 * <p>当前为同步分发；如需异步，可注入线程池另建异步发布器。
 *
 * @author zhush
 */
public class AgentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AgentEventPublisher.class);

    private final List<AgentEventListener> listeners;

    public AgentEventPublisher(List<AgentEventListener> listeners) {
        this.listeners = listeners == null ? List.of() : listeners;
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

    /**
     * 是否存在监听者。
     *
     * @return 有监听者返回 true
     */
    public boolean hasListeners() {
        return listeners != null && !listeners.isEmpty();
    }
}
