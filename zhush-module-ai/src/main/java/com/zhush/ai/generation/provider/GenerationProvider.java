package com.zhush.ai.generation.provider;

import com.zhush.ai.enums.AiTaskType;

/**
 * 生成供应商 SPI：把「提交任务 / 查询状态」抽象成与具体厂商（DashScope / 后续可扩 OpenAI 等）无关的接口。
 *
 * <p>一期只实现 {@link DashScopeGenerationProvider}。图像与视频共用同一套「异步提交 → 轮询查询」状态机，
 * 差异（阻塞 vs 异步、参数集、超时/轮询节奏）由 {@link #submit} / {@link #fetch} 内部按 {@code type} 分派。
 *
 * <p>约定：Provider 不做落库、不做转存、不查模型凭证（凭证在入参 {@link GenerationRequest} 里），
 * 只负责与供应商 SDK/HTTP 交互并归一化状态，便于离线单测。
 *
 * @author zhush
 */
public interface GenerationProvider {

    /**
     * 供应商标识（与 {@code ai_model.type} 对齐，如 dashscope）。
     */
    String name();

    /**
     * 是否支持该任务类型（图像/视频）。
     */
    boolean supports(AiTaskType type);

    /**
     * 异步提交生成任务，立即返回供应商任务号（不阻塞等待产物）。
     */
    GenerationSubmitResult submit(GenerationRequest request);

    /**
     * 按供应商任务号查询当前状态与产物地址。
     *
     * @param type         任务类型（图像/视频走不同查询）
     * @param apiKey       明文 Key
     * @param remoteTaskId 供应商任务号
     */
    GenerationStatusResult fetch(AiTaskType type, String apiKey, String remoteTaskId);
}
