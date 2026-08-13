package com.zhush.protection.idempotent.config;

import com.zhush.protection.idempotent.aspect.IdempotentAspect;
import com.zhush.protection.idempotent.core.IdempotentUserResolver;
import com.zhush.redis.core.RedisCache;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 幂等性自动配置
 *
 * <p>装配 {@link IdempotentAspect} 切面；可通过 {@code zhush.idempotent.enabled=false} 关闭。
 * 当前用户解析器由业务侧/Sa-Token 子配置提供，未提供时按匿名用户处理
 * （此时「用户维度」退化为全局维度，建议仅用于免登录接口的接口维度去重）。
 *
 * @author zhush
 */
@AutoConfiguration
@ConditionalOnClass(RedisCache.class)
@ConditionalOnProperty(prefix = "zhush.idempotent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IdempotentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IdempotentAspect idempotentAspect(RedisCache redisCache,
                                             ObjectProvider<IdempotentUserResolver> userResolverProvider) {
        IdempotentUserResolver userResolver =
                userResolverProvider.getIfAvailable(() -> () -> IdempotentUserResolver.ANONYMOUS);
        return new IdempotentAspect(redisCache, userResolver);
    }

}
