package com.zhush.agentscope.event;

/**
 * 业务侧扩展点：监听会话事件（落库、推送、监控告警）。
 *
 * <p>默认只处理 {} 方法为空的实现；业务按需实现感兴趣的 {@link AgentEventType}。
 *
 * @author zhush
 */
public interface AgentEventListener {

    /**
     * 收到事件时回调。
     *
     * @param event 会话事件
     */
    void onEvent(AgentEvent event);
}
