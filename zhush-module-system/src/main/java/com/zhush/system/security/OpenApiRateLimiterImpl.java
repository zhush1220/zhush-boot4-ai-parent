package com.zhush.system.security;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import com.zhush.oauth2.config.OAuth2ServerProperties;
import com.zhush.oauth2.core.OpenApiRateLimiter;
import com.zhush.system.entity.SysOauth2Client;
import com.zhush.system.mapper.SysOauth2ClientMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 开放 API 限流器（Redis 固定窗口计数）
 *
 * <p>按 {@code clientId} 双重限制：秒级 QPS + 自然日调用配额，阈值取
 * {@code sys_oauth2_client.qps_limit}/{@code daily_quota}，为空时回退
 * {@code zhush.oauth2.default-qps-limit}/{@code default-daily-quota} 全局默认，
 * 0 或负数表示不限制。任一超限时 {@code OpenApiInterceptor} 返回 429。
 *
 * <p>实现要点：
 * <ul>
 *     <li>计数器基于 Redisson {@code RAtomicLong}（INCR + 首次置 TTL），多实例部署下计数全局一致；</li>
 *     <li>客户端限流阈值本地缓存 60 秒，避免每次调用都查库（阈值调整最迟 1 分钟生效）；</li>
 *     <li>Redis 异常时放行（fail-open），限流是保护手段，不应成为开放 API 的单点故障。</li>
 * </ul>
 *
 * @author zhush
 */
@Slf4j
@Component
public class OpenApiRateLimiterImpl implements OpenApiRateLimiter {

    private static final String QPS_KEY_PREFIX = "open_api:limit:qps:";
    private static final String QUOTA_KEY_PREFIX = "open_api:limit:quota:";
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 限流阈值本地缓存有效期（毫秒）
     */
    private static final long RULE_CACHE_TTL_MILLIS = 60_000L;

    private final SysOauth2ClientMapper clientMapper;
    private final RedissonClient redissonClient;
    private final OAuth2ServerProperties properties;

    /**
     * clientId → 限流阈值 的本地缓存，降低高频调用下的 DB 压力
     */
    private final TimedCache<String, LimitRule> ruleCache = CacheUtil.newTimedCache(RULE_CACHE_TTL_MILLIS);

    public OpenApiRateLimiterImpl(SysOauth2ClientMapper clientMapper,
                                  RedissonClient redissonClient,
                                  OAuth2ServerProperties properties) {
        this.clientMapper = clientMapper;
        this.redissonClient = redissonClient;
        this.properties = properties;
    }

    @Override
    public boolean tryAcquire(String clientId) {
        try {
            LimitRule rule = ruleCache.get(clientId, () -> loadRule(clientId));

            // 1. 秒级 QPS：固定窗口（当前秒）计数
            if (rule.qpsLimit() > 0 && !tryAcquireQps(clientId, rule.qpsLimit())) {
                log.info("[OpenApi] 客户端 {} 触发 QPS 限流（上限 {}/s）", clientId, rule.qpsLimit());
                return false;
            }

            // 2. 日配额：自然日计数，跨日自动重置
            if (rule.dailyQuota() > 0 && !tryAcquireDailyQuota(clientId, rule.dailyQuota())) {
                log.info("[OpenApi] 客户端 {} 触发日配额限流（上限 {}/日）", clientId, rule.dailyQuota());
                return false;
            }
            return true;
        } catch (Exception e) {
            // fail-open：限流组件故障不阻断开放 API
            log.warn("[OpenApi] 限流校验异常，本次放行，clientId={}", clientId, e);
            return true;
        }
    }

    // ================ 内部方法 ================

    /**
     * 当前秒窗口计数，超过 QPS 上限拒绝
     */
    private boolean tryAcquireQps(String clientId, int qpsLimit) {
        long epochSecond = System.currentTimeMillis() / 1000;
        RAtomicLong counter = redissonClient.getAtomicLong(QPS_KEY_PREFIX + clientId + ":" + epochSecond);
        long count = counter.incrementAndGet();
        if (count == 1) {
            // 秒级窗口留 2 秒 TTL，覆盖时钟毫秒级偏差后自动清理
            counter.expire(Duration.ofSeconds(2));
        }
        return count <= qpsLimit;
    }

    /**
     * 自然日窗口计数，超过日配额拒绝
     */
    private boolean tryAcquireDailyQuota(String clientId, int dailyQuota) {
        String day = LocalDate.now().format(DAY_FORMATTER);
        RAtomicLong counter = redissonClient.getAtomicLong(QUOTA_KEY_PREFIX + clientId + ":" + day);
        long count = counter.incrementAndGet();
        if (count == 1) {
            // 存活至次日零点后 1 小时，冗余窗口便于跨日排查当日用量
            Duration ttl = Duration.between(LocalDateTime.now(), LocalDate.now().plusDays(1).atStartOfDay())
                    .plusHours(1);
            counter.expire(ttl);
        }
        return count <= dailyQuota;
    }

    /**
     * 加载客户端限流阈值：客户端字段为空回退全局默认
     *
     * <p>拦截器在限流前已校验客户端启用状态，此处查不到（缓存窗口内被删除）按全局默认兜底。
     */
    private LimitRule loadRule(String clientId) {
        SysOauth2Client client = clientMapper.selectByClientId(clientId);
        int qpsLimit = client != null && client.getQpsLimit() != null
                ? client.getQpsLimit() : properties.getDefaultQpsLimit();
        int dailyQuota = client != null && client.getDailyQuota() != null
                ? client.getDailyQuota() : properties.getDefaultDailyQuota();
        return new LimitRule(qpsLimit, dailyQuota);
    }

    /**
     * 客户端限流阈值（0 或负数表示对应维度不限制）
     */
    private record LimitRule(int qpsLimit, int dailyQuota) {
    }

}
