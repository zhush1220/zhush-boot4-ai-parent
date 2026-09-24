package com.zhush.ai.service;

import com.zhush.agentscope.config.AgentScopeProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link AiRateLimiter} 降级契约测试（方案 §4.2）：
 * 开关关闭 / Redis 未装配 / Redis 异常均放行，仅「明确取不到令牌」才拒绝——
 * 限流是成本保护措施，不允许因组件故障阻断对话主链路。
 */
@ExtendWith(MockitoExtension.class)
class AiRateLimiterTest {

    @Mock
    private ObjectProvider<RedissonClient> redissonProvider;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RRateLimiter rateLimiter;

    private AiRateLimiter limiter(AgentScopeProperties properties) {
        return new AiRateLimiter(redissonProvider, properties);
    }

    private AgentScopeProperties properties(boolean enabled, int ratePerMinute) {
        AgentScopeProperties props = new AgentScopeProperties();
        props.getQuota().setEnabled(enabled);
        props.getQuota().setRatePerMinute(ratePerMinute);
        return props;
    }

    @Test
    void disabledOrZeroRateShouldSkipRedisEntirely() {
        limiter(properties(false, 10)).acquire(1L, "member");
        limiter(properties(true, 0)).acquire(1L, "member");

        verifyNoInteractions(redissonProvider);
    }

    @Test
    void missingRedissonShouldDegradeToPass() {
        when(redissonProvider.getIfAvailable()).thenReturn(null);

        assertThatCode(() -> limiter(properties(true, 10)).acquire(1L, "member"))
                .doesNotThrowAnyException();
    }

    @Test
    void redisFailureShouldDegradeToPass() {
        when(redissonProvider.getIfAvailable()).thenReturn(redissonClient);
        when(redissonClient.getRateLimiter("ai:rate:member:1")).thenThrow(new IllegalStateException("redis down"));

        assertThatCode(() -> limiter(properties(true, 10)).acquire(1L, "member"))
                .doesNotThrowAnyException();
    }

    @Test
    void tokenExhaustedShouldReject() {
        when(redissonProvider.getIfAvailable()).thenReturn(redissonClient);
        when(redissonClient.getRateLimiter("ai:rate:member:1")).thenReturn(rateLimiter);
        when(rateLimiter.tryAcquire(1)).thenReturn(false);

        assertThatThrownBy(() -> limiter(properties(true, 10)).acquire(1L, "member"))
                .isInstanceOf(com.zhush.common.exception.ServiceException.class);
    }
}
