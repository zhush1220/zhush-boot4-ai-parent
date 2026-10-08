package com.zhush.ai.service;

import com.zhush.ai.context.AiSessionContextResolver;
import com.zhush.ai.enums.AiTaskType;
import com.zhush.ai.param.AgentIntentParam;
import com.zhush.ai.vo.AgentIntentVO;
import com.zhush.agentscope.config.AgentScopeProperties;
import com.zhush.agentscope.context.AgentContext;
import com.zhush.agentscope.session.AgentSessionManager;
import com.zhush.redis.core.RedisCache;
import io.agentscope.extensions.judge.jev.ChoiceAnswer;
import io.agentscope.extensions.judge.jev.JevClient;
import io.agentscope.extensions.judge.jev.JevException;
import io.agentscope.extensions.judge.jev.SystemOneRequest;
import io.agentscope.extensions.judge.jev.SystemOneResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AiIntentRouterService} 降级契约测试（方案 14.2 / 第七章）：
 * Jev 未装配 / 空答案 / 低置信 / 调用异常均回落 chat 且不抛错；高置信正常返回分类结果。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiIntentRouterServiceTest {

    @Mock
    private JevClient jevClient;
    @Mock
    private ObjectProvider<JevClient> jevProvider;
    @Mock
    private ObjectProvider<RedisCache> redisProvider;
    @Mock
    private AgentSessionManager sessionManager;
    @Mock
    private AiSessionContextResolver contextResolver;
    @Mock
    private JevDecisionRecorder recorder;
    @Mock
    private AiRateLimiter rateLimiter;

    private AgentScopeProperties properties;
    private AiIntentRouterService service;

    @BeforeEach
    void setUp() {
        properties = new AgentScopeProperties();
        when(contextResolver.resolve()).thenReturn(AgentContext.builder()
                .userId(1L).userType("admin").build());
        when(sessionManager.loadHistory(anyString())).thenReturn(List.of());
        service = new AiIntentRouterService(jevProvider, redisProvider, sessionManager,
                contextResolver, properties, recorder, rateLimiter);
    }

    private AgentIntentParam param(String message) {
        AgentIntentParam p = new AgentIntentParam();
        p.setAgentName("ai-chat");
        p.setSessionId("s-1");
        p.setMessage(message);
        return p;
    }

    private SystemOneResult choiceResult(String choice, Double confidence) {
        return new SystemOneResult("jev-latest",
                Map.of("intent", new ChoiceAnswer(choice, Map.of(), confidence)), null);
    }

    @Test
    void clientAbsentShouldDegradeToChatWithoutError() {
        when(jevProvider.getIfAvailable()).thenReturn(null);

        AgentIntentVO vo = service.classify(param("帮我画一只猫"));

        assertThat(vo.isDegraded()).isTrue();
        assertThat(vo.getTaskType()).isEqualTo(AiTaskType.CHAT.getCode());
        verify(rateLimiter).acquire(1L, "admin");
        verifyNoDecision();
    }

    @Test
    void highConfidenceChoiceShouldReturnClassifiedType() {
        when(jevProvider.getIfAvailable()).thenReturn(jevClient);
        when(jevClient.systemOneBlocking(any(SystemOneRequest.class)))
                .thenReturn(choiceResult("image", 0.93d));
        when(redisProvider.getIfAvailable()).thenReturn(null);

        AgentIntentVO vo = service.classify(param("帮我画一只戴帽子的猫"));

        assertThat(vo.isDegraded()).isFalse();
        assertThat(vo.getTaskType()).isEqualTo("image");
        assertThat(vo.getTaskTypeName()).isEqualTo("图像生成");
        assertThat(vo.getConfidence()).isEqualTo(java.math.BigDecimal.valueOf(0.93d));
        verify(recorder).recordIntent(anyString(), anyString(), anyLong(), anyString(),
                anyString(), any(), org.mockito.ArgumentMatchers.eq(false),
                org.mockito.ArgumentMatchers.eq(false), anyLong(), isNull());
    }

    @Test
    void lowConfidenceShouldFallBackToChatAsDegraded() {
        when(jevProvider.getIfAvailable()).thenReturn(jevClient);
        when(jevClient.systemOneBlocking(any(SystemOneRequest.class)))
                .thenReturn(choiceResult("video", 0.42d));

        AgentIntentVO vo = service.classify(param("随便看看"));

        assertThat(vo.getTaskType()).isEqualTo(AiTaskType.CHAT.getCode());
        assertThat(vo.isDegraded()).isTrue();
    }

    @Test
    void jevExceptionShouldDegradeSilently() {
        when(jevProvider.getIfAvailable()).thenReturn(jevClient);
        when(jevClient.systemOneBlocking(any(SystemOneRequest.class)))
                .thenThrow(new JevException("upstream 503"));

        AgentIntentVO vo = service.classify(param("帮我画一只猫"));

        assertThat(vo.isDegraded()).isTrue();
        assertThat(vo.getTaskType()).isEqualTo(AiTaskType.CHAT.getCode());
        verify(recorder).recordIntent(anyString(), anyString(), anyLong(), anyString(),
                anyString(), isNull(), anyBoolean(), anyBoolean(), anyLong(), anyString());
    }

    @Test
    void emptyAnswersShouldDegradeToChat() {
        when(jevProvider.getIfAvailable()).thenReturn(jevClient);
        when(jevClient.systemOneBlocking(any(SystemOneRequest.class)))
                .thenReturn(new SystemOneResult("jev-latest", Map.of(), null));

        AgentIntentVO vo = service.classify(param("帮我画一只猫"));

        assertThat(vo.isDegraded()).isTrue();
        assertThat(vo.getTaskType()).isEqualTo(AiTaskType.CHAT.getCode());
    }

    private void verifyNoDecision() {
        verify(jevClient, never()).systemOneBlocking(any(SystemOneRequest.class));
    }
}
