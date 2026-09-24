package com.zhush.ai.service;

import com.mybatisflex.core.update.UpdateChain;
import com.zhush.ai.entity.AiGenerationTaskEntity;
import com.zhush.ai.enums.AiTaskType;
import com.zhush.ai.generation.RemoteFileFetcher;
import com.zhush.ai.generation.provider.GenerationProvider;
import com.zhush.ai.generation.provider.GenerationProviderRegistry;
import com.zhush.ai.generation.provider.GenerationSubmitResult;
import com.zhush.ai.mapper.AiChatMessageMapper;
import com.zhush.ai.mapper.AiGenerationTaskMapper;
import com.zhush.agentscope.config.AgentScopeProperties;
import com.zhush.agentscope.context.AgentContext;
import com.zhush.agentscope.model.AgentModelResolver;
import com.zhush.agentscope.model.ModelConfig;
import com.zhush.file.core.FileRecordStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AiGenerationTaskService#submit} 提交路径测试（方案 §5.1 / R2「生产禁 Mock」）：
 * <ul>
 *   <li>无启用态生成模型 → 直接拒绝且<b>不落任务行</b>（不产生脏数据）；</li>
 *   <li>供应商拒绝接单 → 任务置失败并向调用方抛业务异常。</li>
 * </ul>
 *
 * <p>轮询/转存收敛依赖线程池与供应商交互，属集成验证范围，此处不触网、不起线程池。
 */
@ExtendWith(MockitoExtension.class)
class AiGenerationTaskServiceTest {

    @Mock
    private AiGenerationTaskMapper taskMapper;
    @Mock
    private AiChatMessageMapper messageMapper;
    @Mock
    private GenerationProviderRegistry providerRegistry;
    @Mock
    private AgentModelResolver modelResolver;
    @Mock
    private RemoteFileFetcher remoteFileFetcher;
    @Mock
    private FileRecordStore fileRecordStore;
    @Mock
    private AiQuotaService quotaService;

    private AiGenerationTaskService service;

    @BeforeEach
    void setUp() {
        // 不调用 init()：无 workers/sweeper，提交失败路径不依赖线程池
        service = new AiGenerationTaskService(taskMapper, messageMapper, providerRegistry, modelResolver,
                remoteFileFetcher, fileRecordStore, quotaService, new AgentScopeProperties(), new ObjectMapper());
    }

    private AgentContext ctx() {
        return AgentContext.builder().sessionId("s-1").userId(7L).userType("member").build();
    }

    @Test
    void submitWithoutEnabledModelShouldRejectAndNotPersist() {
        when(modelResolver.resolveByCapability("image")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(AiTaskType.IMAGE, "cat", "1024*1024", 1, null, ctx()))
                .isInstanceOf(com.zhush.common.exception.ServiceException.class)
                .hasMessageContaining("未配置可用的生成模型");
        verify(taskMapper, never()).insert(any(AiGenerationTaskEntity.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void submitRejectedByProviderShouldThrowSubmitFail() {
        ModelConfig model = mock(ModelConfig.class);
        lenient().when(model.getType()).thenReturn("dashscope");
        lenient().when(model.getModel()).thenReturn("wan2.2-t2i-plus");
        lenient().when(model.getApiKey()).thenReturn("sk-test");
        when(modelResolver.resolveByCapability("image")).thenReturn(Optional.of(model));
        GenerationProvider provider = mock(GenerationProvider.class);
        when(providerRegistry.get("dashscope")).thenReturn(provider);
        when(provider.name()).thenReturn("dashscope");
        when(provider.submit(any())).thenReturn(GenerationSubmitResult.fail("Throttling", "queue full"));

        // markFailed 内部 UpdateChain.create(mock mapper) 无法解析实体泛型（纯单测环境限制，
        // 真实 mapper 无此问题），静态隔离掉失败落库链路，只验证提交拒绝的对外语义
        try (MockedStatic<UpdateChain> updateChain = mockStatic(UpdateChain.class)) {
            updateChain.when(() -> UpdateChain.create(taskMapper))
                    .thenReturn(mock(UpdateChain.class, RETURNS_DEEP_STUBS));

            assertThatThrownBy(() -> service.submit(AiTaskType.IMAGE, "cat", "1024*1024", 1, null, ctx()))
                    .isInstanceOf(com.zhush.common.exception.ServiceException.class)
                    .hasMessageContaining("生成");
        }
        verify(taskMapper).insert(any(AiGenerationTaskEntity.class));
        verify(quotaService, never()).recordGeneration(any(), any(), any());
    }
}
