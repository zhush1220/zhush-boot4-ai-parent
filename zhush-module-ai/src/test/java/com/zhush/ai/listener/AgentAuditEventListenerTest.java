package com.zhush.ai.listener;

import com.zhush.ai.entity.AgentAuditLogEntity;
import com.zhush.ai.mapper.AgentAuditLogMapper;
import com.zhush.ai.mapper.AiChatSessionMapper;
import com.zhush.agentscope.event.AgentToolCalledEvent;
import com.zhush.agentscope.event.AgentToolResultEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@code web_fetch} 正文不落库测试（方案 §6.5 / §10 决策 7）：
 *
 * <p>抓取回包的 {@code text} 是外部网页原文，可能含版权或敏感内容，审计只允许记规模与元数据；
 * 其余工具（{@code web_search} 摘要等）按原有长度截断口径落库，不做字段摘除。
 * 这条同时守住"降级回包（无 text 字段）不得被整体丢弃"——超限原因等业务信息仍需可查。
 */
@ExtendWith(MockitoExtension.class)
class AgentAuditEventListenerTest {

    @Mock
    private AgentAuditLogMapper auditLogMapper;
    @Mock
    private AiChatSessionMapper sessionMapper;

    private AgentAuditEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new AgentAuditEventListener(auditLogMapper, sessionMapper, new ObjectMapper());
        when(sessionMapper.selectOneByQuery(any())).thenReturn(null);
    }

    private AgentAuditLogEntity captured() {
        ArgumentCaptor<AgentAuditLogEntity> captor = ArgumentCaptor.forClass(AgentAuditLogEntity.class);
        verify(auditLogMapper).insert(captor.capture());
        return captor.getValue();
    }

    private String fetchResult(String text) {
        return "{\"ok\":true,\"url\":\"https://example.com/a\",\"httpStatus\":200,"
                + "\"contentType\":\"text/html\",\"chars\":" + text.length() + ",\"truncated\":false,"
                + "\"text\":\"" + text + "\",\"hint\":\"外部网页原文\"}";
    }

    private void onToolResult(String toolName, String result) {
        listener.onEvent(AgentToolResultEvent.builder()
                .agentName("ai-chat")
                .sessionId("s-1")
                .userId(7L)
                .name(toolName)
                .result(result)
                .ok(true)
                .build());
    }

    @Test
    void webFetchResultShouldDropBodyButKeepMetadata() {
        onToolResult("web_fetch", fetchResult("气象台发布暴雨红色预警原文"));

        AgentAuditLogEntity entity = captured();
        assertThat(entity.getEventType()).isEqualTo("TOOL_RESULT");
        assertThat(entity.getToolResult()).doesNotContain("气象台发布暴雨红色预警原文")
                .contains("正文已省略")
                .contains("https://example.com/a")
                .contains("\"httpStatus\":200");
    }

    @Test
    void webFetchDegradeResultWithoutBodyShouldKeepReason() {
        onToolResult("web_fetch", "{\"ok\":false,\"reason\":\"今日联网额度已用完（200 次/日）\",\"hint\":\"未能联网\"}");

        AgentAuditLogEntity entity = captured();
        assertThat(entity.getToolResult()).contains("今日联网额度已用完");
    }

    @Test
    void webFetchUnstructuredResultShouldBeOmittedEntirely() {
        onToolResult("web_fetch", "这是一段没有包装成 JSON 的网页正文");

        assertThat(captured().getToolResult()).doesNotContain("网页正文").startsWith("<");
    }

    @Test
    void otherToolResultShouldOnlyBeLengthTruncated() {
        String longSnippet = "x".repeat(5000);
        onToolResult("web_search", "{\"ok\":true,\"snippet\":\"" + longSnippet + "\"}");

        AgentAuditLogEntity entity = captured();
        assertThat(entity.getToolResult()).hasSize(4000).startsWith("{\"ok\":true,\"snippet\":\"xxx");
    }

    @Test
    void toolCalledEventShouldApplySameRedaction() {
        listener.onEvent(AgentToolCalledEvent.builder()
                .agentName("ai-chat")
                .sessionId("s-1")
                .userId(7L)
                .toolName("web_fetch")
                .arguments("{\"url\":\"https://example.com/a\"}")
                .result(fetchResult("页面原文里的内部系统地址 10.0.0.8"))
                .build());

        AgentAuditLogEntity entity = captured();
        assertThat(entity.getEventType()).isEqualTo("TOOL_CALLED");
        assertThat(entity.getToolArgs()).contains("https://example.com/a");
        assertThat(entity.getToolResult()).doesNotContain("10.0.0.8").contains("正文已省略");
    }
}
