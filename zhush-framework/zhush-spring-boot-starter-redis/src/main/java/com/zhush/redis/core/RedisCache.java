package com.zhush.redis.core;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis 缓存操作封装
 *
 * <p>基于 {@link RedisTemplate}（String key + JSON value 序列化）封装常用缓存操作：
 * 字符串/对象、Hash、List、Set、ZSet、计数器、过期控制、SCAN 删除等；
 * 值以 JSON（带类型信息）存储，读取时自动还原为原始类型。
 *
 * <p>分布式锁基于 {@link RedissonClient} 实现（未引入 Redisson 时锁方法不可用），
 * 推荐使用 {@link #executeWithLock(String, Duration, Duration, Supplier)} 模板方法,
 * 自动完成加锁、释放，避免遗漏 unlock。
 *
 * @author zhush
 */
@Slf4j
public class RedisCache {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Redisson 客户端（分布式锁），可能为 null（未引入 Redisson 时）
     */
    private final RedissonClient redissonClient;

    public RedisCache(RedisTemplate<String, Object> redisTemplate, RedissonClient redissonClient) {
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
    }

    // ==================== 通用 ====================

    /**
     * 判断 key 是否存在
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 删除单个 key
     */
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    /**
     * 批量删除 key
     */
    public long delete(Collection<String> keys) {
        Long count = redisTemplate.delete(keys);
        return count == null ? 0L : count;
    }

    /**
     * 按模式删除 key（基于 SCAN 实现，避免 KEYS 阻塞；如 "user:*"）
     *
     * @return 删除的 key 数量
     */
    public long deleteByPattern(String pattern) {
        List<String> keys = new ArrayList<>();
        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match(pattern).count(500).build())) {
            cursor.forEachRemaining(keys::add);
        }
        return keys.isEmpty() ? 0L : delete(keys);
    }

    /**
     * 设置过期时间
     */
    public boolean expire(String key, Duration timeout) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout));
    }

    /**
     * 获取剩余过期时间（秒；-1 永不过期，-2 key 不存在）
     */
    public long getExpire(String key) {
        Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return expire == null ? -2L : expire;
    }

    // ==================== String / 对象 ====================

    /**
     * 写入缓存（永不过期）
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 写入缓存并设置过期时间
     */
    public void set(String key, Object value, Duration timeout) {
        redisTemplate.opsForValue().set(key, value, timeout);
    }

    /**
     * 不存在时写入（SET NX，常用于幂等/简单互斥）
     */
    public boolean setIfAbsent(String key, Object value, Duration timeout) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, timeout));
    }

    /**
     * 读取缓存（JSON 反序列化自动还原原始类型）
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    /**
     * 读取缓存，未命中时通过 loader 加载并回填（Cache-Aside 模板）
     */
    public <T> T get(String key, Duration timeout, Supplier<T> loader) {
        T value = get(key);
        if (value != null) {
            return value;
        }
        value = loader.get();
        if (value != null) {
            set(key, value, timeout);
        }
        return value;
    }

    /**
     * 计数器自增（key 不存在时从 0 开始）
     */
    public long increment(String key, long delta) {
        Long result = redisTemplate.opsForValue().increment(key, delta);
        return result == null ? 0L : result;
    }

    /**
     * 计数器自减
     */
    public long decrement(String key, long delta) {
        Long result = redisTemplate.opsForValue().decrement(key, delta);
        return result == null ? 0L : result;
    }

    // ==================== Hash ====================

    /**
     * 写入 Hash 单个字段
     */
    public void hSet(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    /**
     * 批量写入 Hash 字段
     */
    public void hSetAll(String key, Map<String, Object> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    /**
     * 读取 Hash 单个字段
     */
    @SuppressWarnings("unchecked")
    public <T> T hGet(String key, String hashKey) {
        return (T) redisTemplate.opsForHash().get(key, hashKey);
    }

    /**
     * 读取 Hash 全部字段
     */
    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 删除 Hash 字段
     */
    public long hDelete(String key, Object... hashKeys) {
        return redisTemplate.opsForHash().delete(key, hashKeys);
    }

    /**
     * 判断 Hash 字段是否存在
     */
    public boolean hHasKey(String key, String hashKey) {
        return redisTemplate.opsForHash().hasKey(key, hashKey);
    }

    // ==================== List ====================

    /**
     * 从左侧入队
     */
    public long lPush(String key, Object value) {
        Long size = redisTemplate.opsForList().leftPush(key, value);
        return size == null ? 0L : size;
    }

    /**
     * 从右侧出队（可用于简单队列消费）
     */
    @SuppressWarnings("unchecked")
    public <T> T rPop(String key) {
        return (T) redisTemplate.opsForList().rightPop(key);
    }

    /**
     * 读取区间元素（0 到 -1 为全部）
     */
    public List<Object> lRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * List 长度
     */
    public long lSize(String key) {
        Long size = redisTemplate.opsForList().size(key);
        return size == null ? 0L : size;
    }

    // ==================== Set ====================

    /**
     * 添加 Set 元素
     */
    public long sAdd(String key, Object... values) {
        Long count = redisTemplate.opsForSet().add(key, values);
        return count == null ? 0L : count;
    }

    /**
     * 读取 Set 全部元素
     */
    public Set<Object> sMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 判断元素是否在 Set 中
     */
    public boolean sIsMember(String key, Object value) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
    }

    /**
     * 移除 Set 元素
     */
    public long sRemove(String key, Object... values) {
        Long count = redisTemplate.opsForSet().remove(key, values);
        return count == null ? 0L : count;
    }

    // ==================== ZSet ====================

    /**
     * 添加 ZSet 元素（按 score 排序，可用于排行榜）
     */
    public boolean zAdd(String key, Object value, double score) {
        return Boolean.TRUE.equals(redisTemplate.opsForZSet().add(key, value, score));
    }

    /**
     * 获取元素分值
     */
    public Double zScore(String key, Object value) {
        return redisTemplate.opsForZSet().score(key, value);
    }

    /**
     * 按分值升序读取区间元素（0 到 -1 为全部）
     */
    public Set<Object> zRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().range(key, start, end);
    }

    /**
     * 移除 ZSet 元素
     */
    public long zRemove(String key, Object... values) {
        Long count = redisTemplate.opsForZSet().remove(key, values);
        return count == null ? 0L : count;
    }

    // ==================== 分布式锁（Redisson） ====================

    /**
     * 加分布式锁后执行业务并返回结果（自动释放锁）
     *
     * @param lockKey   锁 key
     * @param waitTime  获取锁的最长等待时间
     * @param leaseTime 锁自动释放时间（防止死锁；业务耗时不确定时可传 null 启用看门狗自动续期）
     * @param supplier  业务逻辑
     * @return 业务返回值
     * @throws IllegalStateException 等待超时未获取到锁
     */
    public <T> T executeWithLock(String lockKey, Duration waitTime, Duration leaseTime, Supplier<T> supplier) {
        RLock lock = getLock(lockKey);
        boolean locked;
        try {
            locked = leaseTime == null
                    ? lock.tryLock(waitTime.toMillis(), TimeUnit.MILLISECONDS)
                    : lock.tryLock(waitTime.toMillis(), leaseTime.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("获取分布式锁被中断: " + lockKey, e);
        }
        if (!locked) {
            throw new IllegalStateException("获取分布式锁超时: " + lockKey);
        }
        try {
            return supplier.get();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 加分布式锁后执行业务（无返回值版本）
     */
    public void executeWithLock(String lockKey, Duration waitTime, Duration leaseTime, Runnable runnable) {
        executeWithLock(lockKey, waitTime, leaseTime, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 获取 Redisson 锁对象（需要 tryLock/联锁等细粒度控制时使用，调用方自行负责释放）
     */
    public RLock getLock(String lockKey) {
        if (redissonClient == null) {
            throw new IllegalStateException("RedissonClient 未装配，无法使用分布式锁");
        }
        return redissonClient.getLock(lockKey);
    }

    /**
     * 获取底层 RedisTemplate（需要执行未封装操作时使用）
     */
    public RedisTemplate<String, Object> getRedisTemplate() {
        return redisTemplate;
    }
}
