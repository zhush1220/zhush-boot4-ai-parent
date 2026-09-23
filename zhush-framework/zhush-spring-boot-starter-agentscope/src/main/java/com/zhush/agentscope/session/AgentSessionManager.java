package com.zhush.agentscope.session;

import java.util.List;
import java.util.UUID;

/**
 * 会话管理器：负责会话创建、快照恢复、消息追加、上下文注入。
 *
 * <p>会话状态透明交给 {@link AgentSessionStore}；TODO：会话维度超时回收由存储层 TTL 承担。
 *
 * @author zhush
 */
public class AgentSessionManager {

    private final AgentSessionStore store;

    public AgentSessionManager(AgentSessionStore store) {
        this.store = store;
    }

    /**
     * 新建会话（生成全局唯一会话 ID）。
     *
     * @param agentName 智能体名
     * @return 会话 ID
     */
    public String newSession(String agentName) {
        return newSession(agentName, false);
    }

    /**
     * 新建会话并钉定来源位（路线 B 会话级灰度）。
     *
     * @param agentName   智能体名
     * @param nativeState 创建时是否启用原生状态（按当时全局开关）
     * @return 会话 ID
     */
    public String newSession(String agentName, boolean nativeState) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        SessionSnapshot snapshot = SessionSnapshot.builder()
                .sessionId(sessionId)
                .agentName(agentName)
                .nativeState(nativeState)
                .messageCount(0)
                .build();
        store.save(sessionId, snapshot);
        return sessionId;
    }

    /**
     * 该会话是否为原生状态会话（不存在时按旧路径返回 false）。
     */
    public boolean isNativeState(String sessionId) {
        return store.load(sessionId).map(SessionSnapshot::isNativeState).orElse(false);
    }

    /**
     * 获取会话快照（不存在时按给定信息创建）。
     *
     * @param sessionId 会话 ID
     * @param agentName 智能体名
     * @return 快照
     */
    public SessionSnapshot getOrCreate(String sessionId, String agentName) {
        return store.load(sessionId)
                .orElseGet(() -> {
                    SessionSnapshot snapshot = SessionSnapshot.builder()
                            .sessionId(sessionId)
                            .agentName(agentName)
                            .messageCount(0)
                            .build();
                    store.save(sessionId, snapshot);
                    return snapshot;
                });
    }

    /**
     * 追加一条消息到会话（同步持久化）。
     *
     * @param sessionId 会话 ID
     * @param message   消息
     */
    public void appendMessage(String sessionId, SessionMessage message) {
        SessionSnapshot snapshot = store.load(sessionId).orElse(null);
        if (snapshot == null) {
            return;
        }
        snapshot.getMessages().add(message);
        snapshot.setMessageCount(snapshot.getMessages().size());
        store.save(sessionId, snapshot);
    }

    /**
     * 恢复会话历史。
     *
     * @param sessionId 会话 ID
     * @return 消息列表（可为空）
     */
    public List<SessionMessage> loadHistory(String sessionId) {
        return store.load(sessionId).map(SessionSnapshot::getMessages).orElseGet(List::of);
    }

    /**
     * 清空会话。
     *
     * @param sessionId 会话 ID
     */
    public void clearSession(String sessionId) {
        store.delete(sessionId);
    }
}
