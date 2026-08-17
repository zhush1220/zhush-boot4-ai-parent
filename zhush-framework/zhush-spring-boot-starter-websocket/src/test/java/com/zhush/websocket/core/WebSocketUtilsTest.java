package com.zhush.websocket.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import tools.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WebSocket 消息推送封装单测（定向推送/广播/在线判断）
 *
 * @author zhush
 */
class WebSocketUtilsTest {

    private WebSocketSessionHolder holder;
    private WebSocketUtils utils;

    @BeforeEach
    void setUp() {
        holder = new WebSocketSessionHolder();
        utils = new WebSocketUtils(holder);
    }

    private WebSocketSession openSession(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    @Test
    void sendToUserOfflineReturnsFalse() {
        assertThat(utils.sendToUser("u1", "hello")).isFalse();
    }

    @Test
    void sendToUserSendsTextMessage() throws IOException {
        WebSocketSession session = openSession("s1");
        holder.add("u1", session);

        assertThat(utils.sendToUser("u1", "hello")).isTrue();

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        assertThat(captor.getValue().getPayload()).isEqualTo("hello");
    }

    @Test
    void sendToUserObjectSerializedToJson() throws IOException {
        WebSocketSession session = openSession("s1");
        holder.add("u1", session);

        utils.sendToUser("u1", Map.of("type", "notice", "content", "hi"));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        // Map.of 迭代顺序不固定，解析后断言内容
        Map<String, Object> parsed = com.zhush.common.utils.json.JacksonUtils.parseObject(
                captor.getValue().getPayload(), new TypeReference<Map<String, Object>>() {
                });
        assertThat(parsed).containsEntry("type", "notice").containsEntry("content", "hi");
    }

    @Test
    void sendToUserIoFailureReturnsFalse() throws IOException {
        WebSocketSession session = openSession("s1");
        doThrow(new IOException("conn lost")).when(session).sendMessage(any(TextMessage.class));
        holder.add("u1", session);

        assertThat(utils.sendToUser("u1", "hello")).isFalse();
    }

    @Test
    void sendToAllCountsOnlyOpenSessions() throws IOException {
        WebSocketSession online = openSession("s1");
        WebSocketSession offline = mock(WebSocketSession.class);
        when(offline.getId()).thenReturn("s2");
        when(offline.isOpen()).thenReturn(false);
        holder.add("u1", online);
        holder.add("u2", offline);

        assertThat(utils.sendToAll("broadcast")).isEqualTo(1);
        verify(online).sendMessage(any(TextMessage.class));
    }

    @Test
    void isOnlineAndOnlineCount() {
        assertThat(utils.isOnline("u1")).isFalse();
        assertThat(utils.onlineCount()).isZero();

        WebSocketSession session = openSession("s1");
        holder.add("u1", session);
        assertThat(utils.isOnline("u1")).isTrue();
        assertThat(utils.onlineCount()).isEqualTo(1);
    }
}
