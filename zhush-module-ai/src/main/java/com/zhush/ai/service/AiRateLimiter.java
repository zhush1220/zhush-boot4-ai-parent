package com.zhush.ai.service;

import com.zhush.agentscope.config.AgentScopeProperties;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static com.zhush.ai.enums.AiErrorCode.AI_RATE_LIMITED;
import com.zhush.common.exception.ServiceExceptionUtil;

/**
 * AI 请求限流器：单用户每分钟请求速率闸门（QPS 维度，与「按日/月次数配额」正交）。
 *
 * <p>基于 Redisson {@link RRateLimiter}（{@code RateType.OVERALL} 全局生效，多实例共享同一计数）。
 * Redis 不可用（未装配 {@link RedissonClient}）时<b>降级放行并告警</b>——限流是体验/成本保护措施，
 * 不应因缓存故障阻断主链路；次数配额由 DB 保证，不依赖此处。
 *
 * @author zhush
 */
@Component
public class AiRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(AiRateLimiter.class);

    private static final String KEY_PREFIX = "ai:rate:";

    private final ObjectProvider<RedissonClient> redissonClientProvider;
    private final AgentScopeProperties properties;

    public AiRateLimiter(ObjectProvider<RedissonClient> redissonClientProvider,
                        AgentScopeProperties properties) {
        this.redissonClientProvider = redissonClientProvider;
        this.properties = properties;
    }

    /**
     * 获取一次令牌，超限抛业务异常。
     *
     * @param userId   用户 ID
     * @param userType 用户类型
     */
    public void acquire(Long userId, String userType) {
        AgentScopeProperties.Quota quota = properties.getQuota();
        if (quota == null || !quota.isEnabled() || quota.getRatePerMinute() <= 0) {
            return;
        }
        RedissonClient redissonClient = redissonClientProvider.getIfAvailable();
        if (redissonClient == null) {
            log.debug("[AiRateLimiter] RedissonClient 未装配，限流降级放行");
            return;
        }
        long uid = userId == null ? 0L : userId;
        String type = userType == null ? "anonymous" : userType;
        String key = KEY_PREFIX + type + ":" + uid;
        try {
            RRateLimiter limiter = redissonClient.getRateLimiter(key);
            // trySetRate 仅在配置不存在时生效，重复调用无副作用
            limiter.trySetRate(RateType.OVERALL, quota.getRatePerMinute(), Duration.ofMinutes(1));
            if (!limiter.tryAcquire(1)) {
                throw ServiceExceptionUtil.exception(AI_RATE_LIMITED);
            }
        } catch (com.zhush.common.exception.ServiceException e) {
            throw e;
        } catch (Exception e) {
            // Redis 抖动等异常降级放行，不因限流组件故障阻断业务
            log.warn("[AiRateLimiter] 限流判定异常，降级放行, key={}, error={}", key, e.getMessage());
        }
    }
}
