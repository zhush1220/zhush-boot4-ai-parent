package com.zhush.ai.generation.provider;

import com.zhush.ai.enums.AiTaskType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DashScopeGenerationProvider} 无网络部分契约测试（方案 §9：不触网，验证归一化语义）：
 * supports 范围、状态串映射全分支、提交结果缺失响应时的失败归一。
 */
class DashScopeGenerationProviderTest {

    private final DashScopeGenerationProvider provider = new DashScopeGenerationProvider();

    private int mapStatus(String status) throws Exception {
        Method m = DashScopeGenerationProvider.class.getDeclaredMethod("mapStatus", String.class);
        m.setAccessible(true);
        return (int) m.invoke(provider, status);
    }

    @Test
    void nameAndSupportsShouldBeStableContract() {
        assertThat(provider.name()).isEqualTo("dashscope");
        assertThat(provider.supports(AiTaskType.IMAGE)).isTrue();
        assertThat(provider.supports(AiTaskType.VIDEO)).isTrue();
        assertThat(provider.supports(AiTaskType.CHAT)).isFalse();
        assertThat(provider.supports(AiTaskType.DOCX)).isFalse();
    }

    @Test
    void mapStatusShouldCoverAllDashScopeStates() throws Exception {
        assertThat(mapStatus("PENDING")).isEqualTo(GenerationStatusResult.STATUS_QUEUED);
        assertThat(mapStatus("RUNNING")).isEqualTo(GenerationStatusResult.STATUS_RUNNING);
        assertThat(mapStatus("suspended")).isEqualTo(GenerationStatusResult.STATUS_RUNNING);
        assertThat(mapStatus("SUCCEEDED")).isEqualTo(GenerationStatusResult.STATUS_SUCCEEDED);
        assertThat(mapStatus("FAILED")).isEqualTo(GenerationStatusResult.STATUS_FAILED);
        assertThat(mapStatus("CANCELED")).isEqualTo(GenerationStatusResult.STATUS_FAILED);
        assertThat(mapStatus("UNKNOWN")).isEqualTo(GenerationStatusResult.STATUS_FAILED);
    }

    @Test
    void mapStatusShouldTreatNullOrUnknownAsRunning() throws Exception {
        // 保守策略：认不出的状态交给超时兜底收敛，不误判失败
        assertThat(mapStatus(null)).isEqualTo(GenerationStatusResult.STATUS_RUNNING);
        assertThat(mapStatus("SOMETHING_NEW")).isEqualTo(GenerationStatusResult.STATUS_RUNNING);
    }

    @Test
    @SuppressWarnings("unchecked")
    void extractImageSubmitShouldFailWithoutResponse() throws Exception {
        Method m = DashScopeGenerationProvider.class
                .getDeclaredMethod("extractImageSubmit", com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult.class);
        m.setAccessible(true);

        GenerationSubmitResult result = (GenerationSubmitResult) m.invoke(provider, (Object) null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMsg()).isEqualTo("无响应");
    }
}
