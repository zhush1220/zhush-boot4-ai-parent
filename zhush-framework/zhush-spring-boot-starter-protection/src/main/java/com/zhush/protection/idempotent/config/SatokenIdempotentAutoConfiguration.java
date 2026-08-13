package com.zhush.protection.idempotent.config;

import com.zhush.protection.idempotent.core.IdempotentUserResolver;
import com.zhush.protection.idempotent.core.SatokenIdempotentUserResolver;
import com.zhush.satoken.core.StpAdminUtil;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 幂等性 Sa-Token 用户解析自动配置
 *
 * <p>项目引入 Sa-Token 时自动装配 {@link SatokenIdempotentUserResolver}，
 * 使「用户维度 / 参数维度」幂等去重能区分不同登录人；
 * 未引入 Sa-Token 时本配置不生效，解析器降级为匿名用户。
 *
 * @author zhush
 */
@AutoConfiguration(before = IdempotentAutoConfiguration.class)
@ConditionalOnClass(StpAdminUtil.class)
@ConditionalOnProperty(prefix = "zhush.idempotent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SatokenIdempotentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IdempotentUserResolver idempotentUserResolver() {
        return new SatokenIdempotentUserResolver();
    }

}
