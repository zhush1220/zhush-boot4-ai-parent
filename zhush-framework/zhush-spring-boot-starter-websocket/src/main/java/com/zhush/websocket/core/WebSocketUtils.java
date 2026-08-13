package com.zhush.websocket.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;

/**
 * WebSocket 消息推送封装
 *
 * <p>提供定向推送与广播能力；会话未打开或发送失败仅记录日志不抛异常，
 * 避免推送失败影响主业务流程。
 *
 * @author zhush
 */
@Slf4j
public class WebSocketUtils {

    private final WebSocketSessionHolder sessionHolder;

    public WebSocketUtils(WebSocketSessionHolder sessionHolder) {
        this.sessionHolder = sessionHolder;
    }

    /**
     * 向指定用户推送文本消息
     *
     * @return 发送成功返回 true；用户不在线或发送失败返回 false
     */
    public boolean sendToUser(String userId, String message) {
        WebSocketSession session = sessionHolder.get(userId);
        return send(session, userId, message);
    }

    /**
     * 向指定用户推送对象消息（JSON 序列化）
     */
    public boolean sendToUser(String userId, Object message) {
        return sendToUser(userId, toJson(message));
    }

    /**
     * 向全部在线用户广播文本消息
     *
     * @return 成功送达的会话数
     */
    public int sendToAll(String message) {
        int success = 0;
        for (Map.Entry<String, WebSocketSession> entry : sessionHolder.all().entrySet()) {
            if (send(entry.getValue(), entry.getKey(), message)) {
                success++;
            }
        }
        return success;
    }

    /**
     * 向全部在线用户广播对象消息（JSON 序列化）
     */
    public int sendToAll(Object message) {
        return sendToAll(toJson(message));
    }

    /**
     * 判断用户是否在线
     */
    public boolean isOnline(String userId) {
        WebSocketSession session = sessionHolder.get(userId);
        return session != null && session.isOpen();
    }

    /**
     * 当前在线人数
     */
    public int onlineCount() {
        return sessionHolder.size();
    }

    private boolean send(WebSocketSession session, String userId, String message) {
        if (session == null || !session.isOpen()) {
            log.debug("WebSocket 推送跳过（不在线）: userId={}", userId);
            return false;
        }
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(message));
            }
            return true;
        } catch (IOException e) {
            log.error("WebSocket 推送失败: userId={}", userId, e);
            return false;
        }
    }

    private String toJson(Object message) {
        if (message instanceof String str) {
            return str;
        }
        return com.zhush.common.utils.json.JacksonUtils.toJsonString(message);
    }
}
