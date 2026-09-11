package com.zhush.satoken.config;

import cn.dev33.satoken.dao.SaTokenDao;
import com.zhush.satoken.core.RedissonSaTokenDao;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Sa-Token 自动配置
 *
 * <p>装配鉴权拦截器与安全配置项。登录用户 id 与审计字段的桥接由 MyBatis starter 完成
 * （见 {@code com.zhush.mybatis} 下的自动配置），因其持有 {@code LoginUserContextHolder}。
 *
 * @author zhush
 */
@AutoConfiguration
@EnableConfigurationProperties(SecurityProperties.class)
public class SaTokenAutoConfiguration {

    @Bean
    public SaTokenConfigure saTokenConfigure(SecurityProperties properties, Environment environment) {
        return new SaTokenConfigure(properties, environment);
    }

    /**
     * 覆盖官方 {@code SaTokenDaoForRedisTemplate}：其 {@code setStringAndKeepTTL} 依赖 {@code SET KEEPTTL}，
     * 与 Redisson 连接不兼容（误传负数过期时间导致登录会话创建失败）。
     * {@link Primary} 保证 Sa-Token 注入本实现而非官方默认实现。
     */
    @Bean
    @Primary
    public SaTokenDao saTokenDao(RedisConnectionFactory connectionFactory) {
        RedissonSaTokenDao dao = new RedissonSaTokenDao();
        dao.init(connectionFactory);
        return dao;
    }

}
