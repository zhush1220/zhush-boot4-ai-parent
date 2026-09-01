package com.zhush.agentscope.session;

import tools.jackson.databind.ObjectMapper;
import com.zhush.redis.core.RedisCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis 会话存储：基于项目 {@link RedisCache}，key 带租户/业务前缀，TTL 自动回收。
 *
 * <p>序列化采用 Jackson（与模块内其它对象一致），存储时以 JSON 字符串落在 Redis，
 * 读取时反序列化，避免与 RedisTemplate 默认序列化器冲突。
 *
 * @author zhush
 */
public class RedisAgentSessionStore implements AgentSessionStore {

    private static final Logger log = LoggerFactory.getLogger(RedisAgentSessionStore.class);

    private static final String KEY_PREFIX = "agentscope:session:";

    private final RedisCache redisCache;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public RedisAgentSessionStore(RedisCache redisCache, ObjectMapper objectMapper, Duration ttl) {
        this.redisCache = redisCache;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
    }

    @Override
    public void save(String sessionId, SessionSnapshot snapshot) {
        try {
            String json = objectMapper.writeValueAsString(snapshot);
            redisCache.set(key(sessionId), json, ttl);
        } catch (Exception e) {
            log.warn("会话保存失败, sessionId={}, error={}", sessionId, e.getMessage());
        }
    }

    @Override
    public Optional<SessionSnapshot> load(String sessionId) {
        try {
            String json = redisCache.get(key(sessionId));
            if (json == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(objectMapper.readValue(json, SessionSnapshot.class));
        } catch (Exception e) {
            log.warn("会话加载失败, sessionId={}, error={}", sessionId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void delete(String sessionId) {
        redisCache.delete(key(sessionId));
    }

    private String key(String sessionId) {
        return KEY_PREFIX + sessionId;
    }
}
