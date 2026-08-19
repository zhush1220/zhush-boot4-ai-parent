package com.zhush.captcha.store;

import cloud.tianai.captcha.cache.CacheStore;
import cloud.tianai.captcha.common.AnyMap;
import com.zhush.redis.core.RedisCache;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的验证码答案存储
 *
 * <p>实现 tianai-captcha 的 {@link CacheStore} 接口，将验证码答案存入 Redis，
 * 支持多实例部署（答案在任一节点生成、任一节点校验）。
 *
 * @author zhush
 */
@Slf4j
public class RedisCacheStore implements CacheStore {

    /**
     * Redis key 前缀（与项目其他 key 风格保持一致）
     */
    private static final String KEY_PREFIX = "zhush:captcha:";

    private final RedisCache redisCache;

    public RedisCacheStore(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    @Override
    public AnyMap getCache(String key) {
        Map<String, Object> map = redisCache.get(KEY_PREFIX + key);
        return map == null ? null : AnyMap.of(map);
    }

    @Override
    public AnyMap getAndRemoveCache(String key) {
        AnyMap cache = getCache(key);
        if (cache != null) {
            redisCache.delete(KEY_PREFIX + key);
        }
        return cache;
    }

    @Override
    public boolean setCache(String key, AnyMap value, Long ttl, TimeUnit unit) {
        try {
            Duration timeout = Duration.ofMillis(unit.toMillis(ttl));
            // 以普通 HashMap 存储，避免序列化依赖 tianai 内部类型
            redisCache.set(KEY_PREFIX + key, new HashMap<String, Object>(value), timeout);
            return true;
        } catch (Exception e) {
            log.error("[验证码] 写入 Redis 失败 key={}", key, e);
            return false;
        }
    }

    @Override
    public Long incr(String key, long delta, Long ttl, TimeUnit unit) {
        String fullKey = KEY_PREFIX + key;
        Long value = redisCache.increment(fullKey, delta);
        if (ttl != null && ttl > 0) {
            redisCache.expire(fullKey, Duration.ofMillis(unit.toMillis(ttl)));
        }
        return value;
    }

    @Override
    public Long getLong(String key) {
        Object value = redisCache.get(KEY_PREFIX + key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    @Override
    public void close() {
        // Redis 生命周期由容器管理，无需处理
    }
}
