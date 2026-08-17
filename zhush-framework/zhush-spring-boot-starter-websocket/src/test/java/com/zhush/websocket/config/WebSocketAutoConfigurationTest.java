package com.zhush.websocket.config;

import com.zhush.test.StarterTestSupport;
import com.zhush.websocket.core.WebSocketSessionHolder;
import com.zhush.websocket.core.WebSocketUtils;
import com.zhush.websocket.core.ZhushWebSocketHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;

/**
 * WebSocket starter 装配冒烟测试（Servlet 环境，开关开/关）
 *
 * @author zhush
 */
class WebSocketAutoConfigurationTest extends StarterTestSupport {

    @Test
    void disabledByDefault() {
        WebApplicationContextRunner runner = webRunner(WebSocketAutoConfiguration.class);
        assertNoBean(runner, WebSocketSessionHolder.class);
        assertNoBean(runner, WebSocketUtils.class);
        assertNoBean(runner, ZhushWebSocketHandler.class);
        assertNoBean(runner, WebSocketConfigurer.class);
    }

    @Test
    void disabledExplicitly() {
        assertNoBean(webRunner(WebSocketAutoConfiguration.class)
                .withPropertyValues("zhush.websocket.enabled=false"), WebSocketUtils.class);
    }

    @Test
    void enabledRegistersAllBeans() {
        WebApplicationContextRunner runner = webRunner(WebSocketAutoConfiguration.class)
                .withPropertyValues("zhush.websocket.enabled=true");
        assertSingleBean(runner, WebSocketSessionHolder.class);
        assertSingleBean(runner, WebSocketUtils.class);
        assertSingleBean(runner, ZhushWebSocketHandler.class);
        assertSingleBean(runner, WebSocketConfigurer.class);
    }
}
