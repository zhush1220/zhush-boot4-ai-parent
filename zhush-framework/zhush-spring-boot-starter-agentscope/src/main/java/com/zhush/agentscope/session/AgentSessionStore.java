package com.zhush.agentscope.session;

import java.util.Optional;

/**
 * 会话状态存储 SPI：默认 Redis 实现，可替换为 DB / 内存。
 *
 * <p>会话维度超时回收由 Redis TTL 承担。
 *
 * @author zhush
 */
public interface AgentSessionStore {

    /**
     * 保存会话快照。
     *
     * @param sessionId 会话 ID
     * @param snapshot  快照
     */
    void save(String sessionId, SessionSnapshot snapshot);

    /**
     * 加载会话快照。
     *
     * @param sessionId 会话 ID
     * @return 快照，不存在返回空
     */
    Optional<SessionSnapshot> load(String sessionId);

    /**
     * 删除会话。
     *
     * @param sessionId 会话 ID
     */
    void delete(String sessionId);
}
