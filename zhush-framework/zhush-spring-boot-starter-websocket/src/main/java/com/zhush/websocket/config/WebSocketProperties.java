package com.zhush.websocket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * WebSocket 配置属性
 *
 * @author zhush
 */
@Data
@ConfigurationProperties(prefix = WebSocketProperties.PREFIX)
public class WebSocketProperties {

    public static final String PREFIX = "zhush.websocket";

    /**
     * 是否启用 WebSocket（默认关闭；关闭时零装配）
     */
    private Boolean enabled = false;

    /**
     * 端点路径（客户端连接 ws://host:port{endpoint}?userId=xxx）
     */
    private String endpoint = "/ws";

    /**
     * 允许跨域的 Origin 列表（默认全部；生产建议收敛）
     */
    private List<String> allowedOrigins = List.of("*");

    /**
     * 心跳间隔（秒），定时发送 Ping 并清理失效连接；<=0 关闭心跳
     */
    private Long heartbeatInterval = 30L;
}
