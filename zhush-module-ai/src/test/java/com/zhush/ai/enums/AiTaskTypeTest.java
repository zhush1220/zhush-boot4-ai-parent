package com.zhush.ai.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AiTaskType} 契约测试（底座 B3）：容错解析、能力映射与生成类判定。
 */
class AiTaskTypeTest {

    @Test
    void ofShouldParseKnownCodesIgnoringCaseAndBlank() {
        assertThat(AiTaskType.of("copywriting")).isEqualTo(AiTaskType.COPYWRITING);
        assertThat(AiTaskType.of(" IMAGE ")).isEqualTo(AiTaskType.IMAGE);
        assertThat(AiTaskType.of("video")).isEqualTo(AiTaskType.VIDEO);
    }

    @Test
    void ofShouldFallBackToChatForUnknownOrEmpty() {
        assertThat(AiTaskType.of(null)).isEqualTo(AiTaskType.CHAT);
        assertThat(AiTaskType.of("")).isEqualTo(AiTaskType.CHAT);
        assertThat(AiTaskType.of("  ")).isEqualTo(AiTaskType.CHAT);
        assertThat(AiTaskType.of("hacker-type")).isEqualTo(AiTaskType.CHAT);
    }

    @Test
    void capabilityShouldMapGenerationTypesOnly() {
        assertThat(AiTaskType.IMAGE.capability()).isEqualTo("image");
        assertThat(AiTaskType.VIDEO.capability()).isEqualTo("video");
        assertThat(AiTaskType.CHAT.capability()).isEqualTo("text");
        assertThat(AiTaskType.COPYWRITING.capability()).isEqualTo("text");
        assertThat(AiTaskType.DOCX.capability()).isEqualTo("text");
        assertThat(AiTaskType.PDF.capability()).isEqualTo("text");
        assertThat(AiTaskType.PARSE.capability()).isEqualTo("text");
    }

    @Test
    void isGenerationShouldOnlyMatchImageAndVideo() {
        assertThat(AiTaskType.IMAGE.isGeneration()).isTrue();
        assertThat(AiTaskType.VIDEO.isGeneration()).isTrue();
        assertThat(AiTaskType.DOCX.isGeneration()).isFalse();
        assertThat(AiTaskType.CHAT.isGeneration()).isFalse();
    }

    @Test
    void codeShouldAlignWithFrontendCapabilityValues() {
        // 前端 ASSISTANT_CAPABILITIES.value 与落库值逐字对齐，改动需两端同步（方案 §2.2/§八）
        assertThat(AiTaskType.values())
                .extracting(AiTaskType::getCode)
                .containsExactly("chat", "copywriting", "docx", "pdf", "parse", "image", "video");
    }
}
