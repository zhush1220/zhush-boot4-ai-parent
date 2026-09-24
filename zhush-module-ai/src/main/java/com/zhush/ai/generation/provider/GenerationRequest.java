package com.zhush.ai.generation.provider;

import com.zhush.ai.enums.AiTaskType;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 生成请求（供应商无关）：封装一次图像/视频生成所需的全部入参与取到的模型凭证。
 *
 * <p>凭证（{@code model} + {@code apiKey} 明文）由 {@code AgentModelResolver.resolveByCapability} 从
 * 启用态模型池取出后注入，Provider 侧不查库、不解密，保证供应商实现纯净化、可单测。
 *
 * @author zhush
 */
@Data
@Builder
public class GenerationRequest {

    /** 任务类型：IMAGE / VIDEO */
    private AiTaskType type;

    /** 供应商标识（ai_model.type，如 dashscope），选择 Provider 实现 */
    private String provider;

    /** 实际模型名（如 wanx2.1-t2i-turbo） */
    private String model;

    /** API Key（已解密明文） */
    private String apiKey;

    /** 提示词 */
    private String prompt;

    /** 反向提示词，可空 */
    private String negativePrompt;

    /** 尺寸（图像 1024*1024 / 视频 1280*720），可空由供应商默认 */
    private String size;

    /** 图像张数（仅 IMAGE），可空 */
    private Integer n;

    /** 视频时长秒（仅 VIDEO），可空 */
    private Integer duration;

    /** 图生视频首帧图地址（可空，一期主要用文生视频） */
    private String imageUrl;

    /** 透传参数（ai_model.options 合并而来），可空 */
    private Map<String, Object> options;
}
