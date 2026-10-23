package com.zhush.ai.service;

import com.zhush.ai.context.AiSessionContextResolver;
import com.zhush.ai.entity.AiChatMessageEntity;
import com.zhush.ai.entity.AiChatSessionEntity;
import com.zhush.ai.enums.AiTaskType;
import com.zhush.ai.mapper.AiChatMessageMapper;
import com.zhush.ai.mapper.AiChatSessionMapper;
import com.zhush.ai.param.AgentChatParam;
import com.zhush.agentscope.agent.AgentRegistry;
import com.zhush.agentscope.agent.AgentReply;
import com.zhush.agentscope.config.AgentScopeProperties;
import com.zhush.agentscope.context.AgentContext;
import com.zhush.agentscope.event.AgentResumeChannels;
import com.zhush.agentscope.template.AgentTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AgentChatService#chat} 前置闸门与 taskType 落库测试（方案 §2.1 / §4.2-4.4）：
 * <ul>
 *   <li>空消息 / 超长消息在进入任何闸门、模型之前即拒；</li>
 *   <li>新建会话触达活跃会话上限时拒绝，不创建会话；</li>
 *   <li>前端 taskType 归一化后随 user/assistant 双消息落库（P0 全链路闭环）。</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AgentChatServiceTest {

    @Mock
    private AgentTemplate agentTemplate;
    @Mock
    private AgentRegistry agentRegistry;
    @Mock
    private AiSessionContextResolver contextResolver;
    @Mock
    private AiChatSessionMapper sessionMapper;
    @Mock
    private AiChatMessageMapper messageMapper;
    @Mock
    private AgentResumeChannels resumeChannels;
    @Mock
    private AiRateLimiter rateLimiter;
    @Mock
    private ComplianceGuard complianceGuard;
    @Mock
    private AiQuotaService quotaService;
    @Mock
    private AgentChatOwnershipGuard ownershipGuard;

    private AgentScopeProperties properties;
    private AgentChatService service;

    @BeforeEach
    void setUp() {
        properties = new AgentScopeProperties();
        service = new AgentChatService(agentTemplate, agentRegistry, contextResolver, sessionMapper,
                messageMapper, new ObjectMapper(), resumeChannels, properties,
                rateLimiter, complianceGuard, quotaService, ownershipGuard);
    }

    private AgentChatParam param(String message, String sessionId, String taskType) {
        AgentChatParam param = new AgentChatParam();
        param.setAgentName("ai-chat");
        param.setMessage(message);
        param.setSessionId(sessionId);
        param.setTaskType(taskType);
        return param;
    }

    private void stubContext() {
        when(contextResolver.resolve()).thenReturn(
                AgentContext.builder().userId(7L).userType("member").build());
    }

    @Test
    void blankMessageShouldRejectBeforeAnyGate() {
        assertThatThrownBy(() -> service.chat(param("  ", null, null)))
                .isInstanceOf(com.zhush.common.exception.ServiceException.class);
        verifyNoRateLimitOrModel();
    }

    @Test
    void overlongMessageShouldRejectByConfiguredLimit() {
        properties.getChat().setMaxMessageLength(5);

        assertThatThrownBy(() -> service.chat(param("123456", null, null)))
                .isInstanceOf(com.zhush.common.exception.ServiceException.class)
                .hasMessageContaining("消息长度超出限制");
        verifyNoRateLimitOrModel();
    }

    @Test
    void newSessionOverActiveLimitShouldRejectWithoutCreating() {
        properties.getQuota().setMaxActiveSessions(2);
        stubContext();
        when(sessionMapper.selectCountByQuery(any())).thenReturn(2L);

        assertThatThrownBy(() -> service.chat(param("hi", null, null)))
                .isInstanceOf(com.zhush.common.exception.ServiceException.class)
                .hasMessageContaining("会话数量已达上限");
        verify(agentTemplate, never()).newSession(anyString());
    }

    @Test
    void frontendTaskTypeShouldNormalizeAndPersistOnBothMessages() {
        stubContext();
        AiChatSessionEntity session = new AiChatSessionEntity();
        session.setSessionId("s-1");
        session.setMessageCount(5);
        when(sessionMapper.selectOneByQuery(any())).thenReturn(session);
        when(agentTemplate.chat(anyString(), anyString(), anyString())).thenReturn(mock(AgentReply.class));

        service.chat(param("画一只猫", "s-1", "IMAGE"));

        ArgumentCaptor<AiChatMessageEntity> captor = ArgumentCaptor.forClass(AiChatMessageEntity.class);
        verify(messageMapper, times(2)).insert(captor.capture());
        List<AiChatMessageEntity> saved = captor.getAllValues();
        assertThat(saved.get(0).getRole()).isEqualTo("user");
        assertThat(saved.get(0).getTaskType()).isEqualTo(AiTaskType.IMAGE.getCode());
        assertThat(saved.get(1).getRole()).isEqualTo("assistant");
        assertThat(saved.get(1).getTaskType()).isEqualTo(AiTaskType.IMAGE.getCode());
        verify(quotaService).record(7L, "member", AiTaskType.IMAGE, 0, 0);
    }

    private void verifyNoRateLimitOrModel() {
        verify(rateLimiter, never()).acquire(any(), any());
        verify(agentTemplate, never()).chat(anyString(), anyString(), anyString());
    }

    /**
     * 本轮联网开关按原值透传：未显式指定时保持 null，由工具层按配置默认值判定
     * （工具集按智能体缓存，摘不掉，只能把开关带到执行处）。
     */
    @Test
    void streamShouldForwardTurnWebSearchSwitchAsGiven() {
        stubContext();
        when(sessionMapper.selectOneByQuery(any())).thenReturn(null);
        when(agentTemplate.stream(anyString(), anyString(), anyString(), any())).thenReturn(Flux.empty());

        AgentChatParam off = param("今天厦门天气", "s-1", null);
        off.setWebSearchEnabled(false);
        service.stream(off);
        service.stream(param("今天厦门天气", "s-2", null));

        verify(agentTemplate).stream("ai-chat", "s-1", "今天厦门天气", false);
        verify(agentTemplate).stream("ai-chat", "s-2", "今天厦门天气", null);
    }
}
