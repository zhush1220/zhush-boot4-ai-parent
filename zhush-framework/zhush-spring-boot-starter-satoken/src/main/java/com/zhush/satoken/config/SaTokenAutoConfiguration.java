package com.zhush.satoken.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

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
    public SaTokenConfigure saTokenConfigure(SecurityProperties properties) {
        return new SaTokenConfigure(properties);
    }

}
