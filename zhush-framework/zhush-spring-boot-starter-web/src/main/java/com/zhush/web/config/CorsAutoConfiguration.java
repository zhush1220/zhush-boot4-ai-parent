package com.zhush.web.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域自动装配
 *
 * <p>基于最高优先级的 {@link CorsFilter} 实现：预检请求（OPTIONS）在 Sa-Token 等
 * 鉴权拦截器之前即被放行，避免前后端分离场景下预检被拦截导致跨域失败。
 *
 * <p>默认开启并放行全部来源；通过 {@code zhush.web.cors.*} 调整来源、方法、请求头等,
 * 设置 {@code zhush.web.cors.enabled=false} 可整体关闭。
 *
 * @author zhush
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(CorsProperties.class)
@ConditionalOnProperty(prefix = CorsProperties.PREFIX, name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class CorsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CorsFilter.class)
    public FilterRegistrationBean<CorsFilter> corsFilterRegistrationBean(CorsProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        // allowCredentials=true 时不允许 allowedOrigins="*"，统一使用 originPatterns
        config.setAllowedOriginPatterns(properties.getAllowedOriginPatterns());
        config.setAllowedMethods(properties.getAllowedMethods());
        config.setAllowedHeaders(properties.getAllowedHeaders());
        if (!properties.getExposedHeaders().isEmpty()) {
            config.setExposedHeaders(properties.getExposedHeaders());
        }
        config.setAllowCredentials(properties.getAllowCredentials());
        config.setMaxAge(properties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(properties.getPathPattern(), config);

        FilterRegistrationBean<CorsFilter> registrationBean =
                new FilterRegistrationBean<>(new CorsFilter(source));
        // 最高优先级：确保先于鉴权等其他 Filter 处理预检请求
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }

}
