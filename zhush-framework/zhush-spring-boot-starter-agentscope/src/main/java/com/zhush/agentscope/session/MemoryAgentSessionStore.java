package com.zhush.agentscope.session;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内会话存储（无 Redis 时的降级实现，适合单机演示 / 测试）。
 *
 * <p>数据不会被其它实例共享，集群部署请使用 {@link RedisAgentSessionStore}。
 *
 * @author zhush
 */
public class MemoryAgentSessionStore implements AgentSessionStore {

    private final Map<String, SessionSnapshot> store = new ConcurrentHashMap<>();

    @Override
    public void save(String sessionId, SessionSnapshot snapshot) {
        store.put(sessionId, snapshot);
    }

    @Override
    public Optional<SessionSnapshot> load(String sessionId) {
        return Optional.ofNullable(store.get(sessionId));
    }

    @Override
    public void delete(String sessionId) {
        store.remove(sessionId);
    }
}
