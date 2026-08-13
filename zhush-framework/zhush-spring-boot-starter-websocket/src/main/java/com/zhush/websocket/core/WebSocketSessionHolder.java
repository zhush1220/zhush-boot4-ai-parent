package com.zhush.websocket.core;

import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 会话注册表
 *
 * <p>维护 userId → 会话 的映射（同一用户重复连接时新会话顶替旧会话）。
 * 集群部署时可替换为 Redis 实现（注册表 + 广播）。
 *
 * @author zhush
 */
public class WebSocketSessionHolder {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * 注册会话（同 userId 已存在时关闭旧会话）
     */
    public void add(String userId, WebSocketSession session) {
        WebSocketSession old = sessions.put(userId, session);
        if (old != null && old.isOpen() && !old.getId().equals(session.getId())) {
            try {
                old.close();
            } catch (Exception ignored) {
                // 旧会话关闭失败不影响新会话注册
            }
        }
    }

    /**
     * 移除会话（仅当注册表中仍是该会话时移除，防止误删新连接）
     */
    public void remove(String userId, WebSocketSession session) {
        sessions.remove(userId, session);
    }

    /**
     * 获取指定用户会话（不在线返回 null）
     */
    public WebSocketSession get(String userId) {
        return sessions.get(userId);
    }

    /**
     * 当前在线会话数
     */
    public int size() {
        return sessions.size();
    }

    /**
     * 全部会话快照（遍历推送用）
     */
    public Map<String, WebSocketSession> all() {
        return Map.copyOf(sessions);
    }
}
