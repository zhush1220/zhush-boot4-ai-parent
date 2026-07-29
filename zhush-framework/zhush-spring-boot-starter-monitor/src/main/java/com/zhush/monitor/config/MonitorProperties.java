package com.zhush.monitor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 监控配置属性
 *
 * @author zhush
 */
@Data
@ConfigurationProperties(prefix = MonitorProperties.PREFIX)
public class MonitorProperties {

    public static final String PREFIX = "zhush.monitor";

    /**
     * 是否启用监控（总开关，默认关闭；开启后内嵌 Spring Boot Admin 服务端）
     */
    private Boolean enabled = false;

    /**
     * 是否装配内嵌 Spring Boot Admin 服务端（默认开启，受总开关约束；
     * 已有独立部署的 Admin Server 时可设为 false，仅作为客户端注册上报）
     */
    private Boolean serverEnabled = true;

}
