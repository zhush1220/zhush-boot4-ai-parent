package com.zhush.websocket.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WebSocket 会话注册表单测（同用户顶替、条件移除）
 *
 * @author zhush
 */
class WebSocketSessionHolderTest {

    private WebSocketSessionHolder holder;

    @BeforeEach
    void setUp() {
        holder = new WebSocketSessionHolder();
    }

    private WebSocketSession session(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        return session;
    }

    @Test
    void addGetSizeAndAll() {
        WebSocketSession s1 = session("s1");
        WebSocketSession s2 = session("s2");
        holder.add("u1", s1);
        holder.add("u2", s2);

        assertThat(holder.get("u1")).isSameAs(s1);
        assertThat(holder.get("missing")).isNull();
        assertThat(holder.size()).isEqualTo(2);
        assertThat(holder.all()).containsExactlyInAnyOrderEntriesOf(Map.of("u1", s1, "u2", s2));
    }

    @Test
    void addReplacesAndClosesOldSession() throws IOException {
        WebSocketSession old = session("old");
        when(old.isOpen()).thenReturn(true);
        WebSocketSession fresh = session("fresh");
        holder.add("u1", old);

        holder.add("u1", fresh);

        verify(old).close();
        assertThat(holder.get("u1")).isSameAs(fresh);
        assertThat(holder.size()).isEqualTo(1);
    }

    @Test
    void addSameSessionTwiceDoesNotClose() throws IOException {
        WebSocketSession session = session("s1");
        when(session.isOpen()).thenReturn(true);
        holder.add("u1", session);

        holder.add("u1", session);

        verify(session, never()).close();
    }

    @Test
    void addDoesNotCloseClosedOldSession() throws IOException {
        WebSocketSession old = session("old");
        when(old.isOpen()).thenReturn(false);
        holder.add("u1", old);

        holder.add("u1", session("fresh"));

        verify(old, never()).close();
    }

    @Test
    void oldSessionCloseFailureDoesNotBlockRegistration() throws IOException {
        WebSocketSession old = session("old");
        when(old.isOpen()).thenReturn(true);
        doThrow(new IOException("already closed")).when(old).close();
        holder.add("u1", old);

        assertThatCode(() -> holder.add("u1", session("fresh"))).doesNotThrowAnyException();
        assertThat(holder.size()).isEqualTo(1);
    }

    @Test
    void removeOnlyRemovesMatchingSession() {
        WebSocketSession s1 = session("s1");
        holder.add("u1", s1);

        holder.remove("u1", session("other"));
        assertThat(holder.get("u1")).isSameAs(s1);

        holder.remove("u1", s1);
        assertThat(holder.get("u1")).isNull();
        assertThat(holder.size()).isZero();
    }
}
