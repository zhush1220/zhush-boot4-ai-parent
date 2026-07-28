package com.zhush.monitor.config;

import de.codecentric.boot.admin.server.config.AdminServerMarkerConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 监控自动装配（Spring Boot Admin）
 *
 * <p>装配内容：
 * <ul>
 *     <li>内嵌 Spring Boot Admin 服务端：注册 Marker Bean 激活 SBA 服务端自动配置,
 *     等价于在启动类标注 {@code @EnableAdminServer}；UI 访问路径由
 *     {@code spring.boot.admin.context-path} 决定（建议 {@code /monitor}）</li>
 *     <li>客户端注册：由 SBA 自身的自动配置驱动,配置
 *     {@code spring.boot.admin.client.url} 指向服务端后即自动注册上报</li>
 * </ul>
 *
 * <p>启停控制：{@code zhush.monitor.enabled}（默认 false,关闭时零装配）；
 * {@code zhush.monitor.server-enabled=false} 可仅作为客户端接入独立部署的 Admin Server。
 *
 * <p>SkyWalking 链路追踪基于 javaagent 挂载,无需装配 Bean：
 * 本 starter 仅引入 {@code apm-toolkit-trace}（@Trace 自定义追踪）与
 * {@code apm-toolkit-logback-1.x}（日志输出 TID）,未挂载 agent 时均为空实现。
 *
 * @author zhush
 */
@AutoConfiguration(beforeName = "de.codecentric.boot.admin.server.config.AdminServerAutoConfiguration")
@EnableConfigurationProperties(MonitorProperties.class)
@ConditionalOnProperty(prefix = MonitorProperties.PREFIX, name = "enabled", havingValue = "true")
public class MonitorAutoConfiguration {

    /**
     * 注册 SBA 服务端 Marker（激活 AdminServerAutoConfiguration）
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(AdminServerMarkerConfiguration.Marker.class)
    @ConditionalOnProperty(prefix = MonitorProperties.PREFIX, name = "server-enabled",
            havingValue = "true", matchIfMissing = true)
    public AdminServerMarkerConfiguration.Marker adminServerMarker() {
        return new AdminServerMarkerConfiguration.Marker();
    }

}
