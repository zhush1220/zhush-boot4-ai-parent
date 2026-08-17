package com.zhush.websocket.config;

import com.zhush.websocket.core.WebSocketMessageHandler;
import com.zhush.websocket.core.WebSocketSessionHolder;
import com.zhush.websocket.core.WebSocketUtils;
import com.zhush.websocket.core.ZhushWebSocketHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 自动装配
 *
 * <p>装配内容：
 * <ul>
 *     <li>端点注册：路径由 {@code zhush.websocket.endpoint} 决定（默认 /ws），
 *     客户端通过 {@code ws://host:port/ws?userId=xxx} 连接，
 *     握手时将 userId 写入会话属性</li>
 *     <li>{@link WebSocketUtils}：定向推送 / 广播 / 在线判断</li>
 *     <li>入站消息：业务侧实现 {@link WebSocketMessageHandler} 并注册为 Bean 即可接管</li>
 * </ul>
 *
 * <p>启停控制：{@code zhush.websocket.enabled}（默认 false，关闭时零装配）。
 *
 * @author zhush
 */
@AutoConfiguration
@ConditionalOnClass(WebSocketHandler.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(WebSocketProperties.class)
@ConditionalOnProperty(prefix = WebSocketProperties.PREFIX, name = "enabled", havingValue = "true")
public class WebSocketAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public WebSocketSessionHolder webSocketSessionHolder() {
        return new WebSocketSessionHolder();
    }

    @Bean
    @ConditionalOnMissingBean
    public ZhushWebSocketHandler zhushWebSocketHandler(WebSocketSessionHolder sessionHolder,
                                                       ObjectProvider<WebSocketMessageHandler> handlerProvider) {
        return new ZhushWebSocketHandler(sessionHolder, handlerProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public WebSocketUtils webSocketUtils(WebSocketSessionHolder sessionHolder) {
        return new WebSocketUtils(sessionHolder);
    }

    /**
     * 端点注册（含 userId 握手拦截器）
     */
    @Bean
    @ConditionalOnMissingBean(WebSocketConfigurer.class)
    public WebSocketConfigurer zhushWebSocketConfigurer(ZhushWebSocketHandler handler,
                                                        WebSocketProperties properties) {
        return registry -> registry.addHandler(handler, properties.getEndpoint())
                .addInterceptors(userIdHandshakeInterceptor())
                .setAllowedOriginPatterns(properties.getAllowedOrigins().toArray(String[]::new));
    }

    /**
     * 握手拦截器：提取 userId 查询参数写入会话属性（缺失时降级为 sessionId）
     */
    private HandshakeInterceptor userIdHandshakeInterceptor() {
        return new HttpSessionHandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                           WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
                if (request instanceof ServletServerHttpRequest servletRequest) {
                    String userId = servletRequest.getServletRequest().getParameter("userId");
                    if (userId != null && !userId.isBlank()) {
                        attributes.put(ZhushWebSocketHandler.USER_ID_ATTR, userId);
                    }
                }
                return super.beforeHandshake(request, response, wsHandler, attributes);
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Exception exception) {
                super.afterHandshake(request, response, wsHandler, exception);
            }
        };
    }
}
