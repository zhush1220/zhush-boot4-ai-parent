package com.zhush.websocket.core;

import org.springframework.web.socket.WebSocketSession;

/**
 * WebSocket 入站消息处理器（可选）
 *
 * <p>业务侧实现并注册为 Spring Bean 后，收到客户端文本消息时回调；
 * 仅做服务端推送时可不必实现。
 *
 * @author zhush
 */
public interface WebSocketMessageHandler {

    /**
     * 处理客户端消息
     *
     * @param userId  连接用户（握手参数 userId）
     * @param message 文本消息内容
     * @param session 会话（可回写响应）
     */
    void handleMessage(String userId, String message, WebSocketSession session);
}
