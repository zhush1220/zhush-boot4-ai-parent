package com.zhush.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 跨域配置属性
 *
 * @author zhush
 */
@Data
@ConfigurationProperties(prefix = CorsProperties.PREFIX)
public class CorsProperties {

    public static final String PREFIX = "zhush.web.cors";

    /**
     * 是否启用跨域处理（默认开启，设为 false 可关闭）
     */
    private Boolean enabled = true;

    /**
     * 生效的路径规则
     */
    private String pathPattern = "/**";

    /**
     * 允许的来源模式（支持通配，如 https://*.zhush.com；默认放行全部）
     */
    private List<String> allowedOriginPatterns = List.of("*");

    /**
     * 允许的请求方法（默认放行全部）
     */
    private List<String> allowedMethods = List.of("*");

    /**
     * 允许的请求头（默认放行全部）
     */
    private List<String> allowedHeaders = List.of("*");

    /**
     * 暴露给前端可读取的响应头（如 Content-Disposition）
     */
    private List<String> exposedHeaders = List.of();

    /**
     * 是否允许携带凭证（Cookie / Authorization 等）
     */
    private Boolean allowCredentials = true;

    /**
     * 预检请求（OPTIONS）结果的缓存时长（秒）
     */
    private Long maxAge = 3600L;
}
