package com.zhush.agentscope.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AgentSessionManager} 会话级灰度来源位测试（S4）：
 * 验证 {@code nativeState} 来源位在创建时钉定、跨消息追加持久保留、未知会话按旧路径降级。
 */
class AgentSessionManagerTest {

    private MemoryAgentSessionStore store;
    private AgentSessionManager manager;

    @BeforeEach
    void setUp() {
        store = new MemoryAgentSessionStore();
        manager = new AgentSessionManager(store);
    }

    @Test
    void newSessionDefaultsToLegacyTrack() {
        String sessionId = manager.newSession("ai-chat");

        assertThat(manager.isNativeState(sessionId)).isFalse();
    }

    @Test
    void newSessionPinsNativeTrackWhenEnabled() {
        String sessionId = manager.newSession("ai-chat", true);

        assertThat(manager.isNativeState(sessionId)).isTrue();
    }

    @Test
    void unknownSessionFallsBackToLegacyTrack() {
        // 会话不存在（如 TTL 过期或跨实例）：按旧路径处理，不阻断
        assertThat(manager.isNativeState("not-exist")).isFalse();
    }

    @Test
    void nativeFlagSurvivesMessageAppend() {
        String sessionId = manager.newSession("ai-chat", true);
        manager.appendMessage(sessionId, SessionMessage.builder().role("user").content("hi").build());
        manager.appendMessage(sessionId, SessionMessage.builder().role("assistant").content("hello").build());

        // load→改→save 往返后来源位不得丢失，且历史完整
        assertThat(manager.isNativeState(sessionId)).isTrue();
        assertThat(manager.loadHistory(sessionId)).hasSize(2);
    }

    @Test
    void clearSessionRemovesTrack() {
        String sessionId = manager.newSession("ai-chat", true);
        manager.clearSession(sessionId);

        assertThat(manager.isNativeState(sessionId)).isFalse();
    }

    @Test
    void twoTracksCoexistIndependently() {
        String legacySession = manager.newSession("ai-chat", false);
        String nativeSession = manager.newSession("ai-chat", true);

        // 会话级灰度：同一进程内新旧会话各按来源位并存，互不污染
        assertThat(manager.isNativeState(legacySession)).isFalse();
        assertThat(manager.isNativeState(nativeSession)).isTrue();
    }
}
