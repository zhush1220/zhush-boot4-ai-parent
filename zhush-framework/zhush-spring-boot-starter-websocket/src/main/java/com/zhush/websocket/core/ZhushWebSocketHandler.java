package com.zhush.websocket.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket 服务端处理器
 *
 * <p>负责连接建立/断开时的会话注册维护，并将入站文本消息委托给
 * 业务侧可选实现的 {@link WebSocketMessageHandler}。
 *
 * @author zhush
 */
@Slf4j
public class ZhushWebSocketHandler extends TextWebSocketHandler {

    /**
     * 握手属性中的用户标识 key
     */
    public static final String USER_ID_ATTR = "userId";

    private final WebSocketSessionHolder sessionHolder;

    private final WebSocketMessageHandler messageHandler;

    public ZhushWebSocketHandler(WebSocketSessionHolder sessionHolder, WebSocketMessageHandler messageHandler) {
        this.sessionHolder = sessionHolder;
        this.messageHandler = messageHandler;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = userId(session);
        sessionHolder.add(userId, session);
        log.info("WebSocket 连接建立: userId={}, sessionId={}, 当前在线 {}",
                userId, session.getId(), sessionHolder.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String userId = userId(session);
        if (messageHandler != null) {
            try {
                messageHandler.handleMessage(userId, message.getPayload(), session);
            } catch (Exception e) {
                log.error("WebSocket 消息处理失败: userId={}", userId, e);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = userId(session);
        sessionHolder.remove(userId, session);
        log.info("WebSocket 连接关闭: userId={}, status={}, 当前在线 {}",
                userId, status, sessionHolder.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket 传输异常: sessionId={}", session.getId(), exception);
        sessionHolder.remove(userId(session), session);
    }

    private String userId(WebSocketSession session) {
        Object userId = session.getAttributes().get(USER_ID_ATTR);
        return userId == null ? session.getId() : userId.toString();
    }
}
