package com.zhush.ai.generation.provider;

import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.alibaba.dashscope.aigc.videosynthesis.VideoSynthesis;
import com.alibaba.dashscope.aigc.videosynthesis.VideoSynthesisParam;
import com.alibaba.dashscope.aigc.videosynthesis.VideoSynthesisResult;
import com.zhush.ai.enums.AiTaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * DashScope（阿里云百炼 / 通义万相）生成供应商实现：基于官方 SDK 的异步任务接口。
 *
 * <p>取证（javap，2.22.9）：{@code ImageSynthesis.asyncCall(ImageSynthesisParam)} 提交、
 * {@code fetch(taskId, apiKey)} 查询，产物在 {@code getOutput().getResults()[]} 的 {@code url}；
 * {@code VideoSynthesis.asyncCall / fetch}，产物在 {@code getOutput().getVideoUrl()}。
 * 二者共用「asyncCall 立即返回 taskId → 轮询 fetch」异步语义（区别于 SDK 自带阻塞式 MultiModalTool）。
 *
 * <p>{@code apiKey} 用<b>明文</b>（由上游从启用态模型池解密得到），与 {@code AiModelService} 口径一致；
 * 所有 SDK 异常在此收敛为归一化失败结果，不外抛，交由业务层统一落状态机。
 *
 * @author zhush
 */
@Component
public class DashScopeGenerationProvider implements GenerationProvider {

    private static final Logger log = LoggerFactory.getLogger(DashScopeGenerationProvider.class);

    private static final String NAME = "dashscope";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean supports(AiTaskType type) {
        return type == AiTaskType.IMAGE || type == AiTaskType.VIDEO;
    }

    @Override
    public GenerationSubmitResult submit(GenerationRequest request) {
        try {
            if (request.getType() == AiTaskType.VIDEO) {
                return submitVideo(request);
            }
            return submitImage(request);
        } catch (Exception e) {
            log.error("[DashScope] 生成任务提交失败, type={}, model={}", request.getType(), request.getModel(), e);
            return GenerationSubmitResult.fail("SUBMIT_ERROR", e.getMessage());
        }
    }

    private GenerationSubmitResult submitImage(GenerationRequest request) throws Exception {
        ImageSynthesis synth = new ImageSynthesis();
        ImageSynthesisParam.ImageSynthesisParamBuilder<?, ?> builder = ImageSynthesisParam.builder()
                .apiKey(request.getApiKey())
                .model(request.getModel())
                .prompt(request.getPrompt());
        if (request.getSize() != null && !request.getSize().isBlank()) {
            builder.size(request.getSize());
        }
        if (request.getN() != null) {
            builder.n(request.getN());
        }
        if (request.getNegativePrompt() != null && !request.getNegativePrompt().isBlank()) {
            builder.negativePrompt(request.getNegativePrompt());
        }
        ImageSynthesisResult result = synth.asyncCall(builder.build());
        return extractImageSubmit(result);
    }

    private GenerationSubmitResult extractImageSubmit(ImageSynthesisResult result) {
        if (result == null || result.getOutput() == null) {
            String code = result == null ? "" : result.getCode();
            String msg = result == null ? "无响应" : result.getMessage();
            return GenerationSubmitResult.fail(code, msg);
        }
        String taskId = result.getOutput().getTaskId();
        if (taskId == null || taskId.isBlank()) {
            return GenerationSubmitResult.fail(result.getOutput().getCode(), result.getOutput().getMessage());
        }
        return GenerationSubmitResult.ok(taskId);
    }

    private GenerationSubmitResult submitVideo(GenerationRequest request) throws Exception {
        VideoSynthesis synth = new VideoSynthesis();
        VideoSynthesisParam.VideoSynthesisParamBuilder<?, ?> builder = VideoSynthesisParam.builder()
                .apiKey(request.getApiKey())
                .model(request.getModel())
                .prompt(request.getPrompt());
        if (request.getSize() != null && !request.getSize().isBlank()) {
            builder.size(request.getSize());
        }
        if (request.getDuration() != null) {
            builder.duration(request.getDuration());
        }
        if (request.getNegativePrompt() != null && !request.getNegativePrompt().isBlank()) {
            builder.negativePrompt(request.getNegativePrompt());
        }
        VideoSynthesisResult result = synth.asyncCall(builder.build());
        if (result == null || result.getOutput() == null) {
            String code = result == null ? "" : result.getCode();
            String msg = result == null ? "无响应" : result.getMessage();
            return GenerationSubmitResult.fail(code, msg);
        }
        String taskId = result.getOutput().getTaskId();
        if (taskId == null || taskId.isBlank()) {
            return GenerationSubmitResult.fail(result.getOutput().getCode(), result.getOutput().getMessage());
        }
        return GenerationSubmitResult.ok(taskId);
    }

    @Override
    public GenerationStatusResult fetch(AiTaskType type, String apiKey, String remoteTaskId) {
        try {
            if (type == AiTaskType.VIDEO) {
                return fetchVideo(apiKey, remoteTaskId);
            }
            return fetchImage(apiKey, remoteTaskId);
        } catch (Exception e) {
            log.error("[DashScope] 生成任务查询失败, type={}, taskId={}", type, remoteTaskId, e);
            return GenerationStatusResult.builder()
                    .status(GenerationStatusResult.STATUS_RUNNING)
                    .errorMsg(e.getMessage())
                    .build();
        }
    }

    private GenerationStatusResult fetchImage(String apiKey, String remoteTaskId) throws Exception {
        ImageSynthesis synth = new ImageSynthesis();
        ImageSynthesisResult result = synth.fetch(remoteTaskId, apiKey);
        if (result == null || result.getOutput() == null) {
            return running(result == null ? null : result.getMessage());
        }
        String status = result.getOutput().getTaskStatus();
        int norm = mapStatus(status);
        if (norm == GenerationStatusResult.STATUS_SUCCEEDED) {
            String url = firstImageUrl(result.getOutput().getResults());
            if (url == null || url.isBlank()) {
                // 状态成功但未取到 URL：视为运行中，等下一轮兜底再查
                return running("产物 URL 为空");
            }
            return GenerationStatusResult.builder()
                    .status(GenerationStatusResult.STATUS_SUCCEEDED)
                    .resultUrl(url)
                    .rawStatus(status)
                    .build();
        }
        if (norm == GenerationStatusResult.STATUS_FAILED) {
            return GenerationStatusResult.builder()
                    .status(GenerationStatusResult.STATUS_FAILED)
                    .rawStatus(status)
                    .errorCode(result.getOutput().getCode())
                    .errorMsg(result.getOutput().getMessage())
                    .build();
        }
        return GenerationStatusResult.builder()
                .status(norm).rawStatus(status).build();
    }

    private String firstImageUrl(List<Map<String, String>> results) {
        if (results == null || results.isEmpty()) {
            return null;
        }
        for (Map<String, String> item : results) {
            if (item == null) {
                continue;
            }
            String url = item.get("url");
            if (url != null && !url.isBlank()) {
                return url;
            }
        }
        return null;
    }

    private GenerationStatusResult fetchVideo(String apiKey, String remoteTaskId) throws Exception {
        VideoSynthesis synth = new VideoSynthesis();
        VideoSynthesisResult result = synth.fetch(remoteTaskId, apiKey);
        if (result == null || result.getOutput() == null) {
            return running(result == null ? null : result.getMessage());
        }
        String status = result.getOutput().getTaskStatus();
        int norm = mapStatus(status);
        if (norm == GenerationStatusResult.STATUS_SUCCEEDED) {
            String url = result.getOutput().getVideoUrl();
            if (url == null || url.isBlank()) {
                return running("产物 URL 为空");
            }
            return GenerationStatusResult.builder()
                    .status(GenerationStatusResult.STATUS_SUCCEEDED)
                    .resultUrl(url)
                    .rawStatus(status)
                    .build();
        }
        if (norm == GenerationStatusResult.STATUS_FAILED) {
            return GenerationStatusResult.builder()
                    .status(GenerationStatusResult.STATUS_FAILED)
                    .rawStatus(status)
                    .errorCode(result.getOutput().getCode())
                    .errorMsg(result.getOutput().getMessage())
                    .build();
        }
        return GenerationStatusResult.builder()
                .status(norm).rawStatus(status).build();
    }

    /**
     * DashScope 任务状态串归一化：PENDING→排队，RUNNING/SUSPENDED→运行，SUCCEEDED→成功，
     * FAILED/CANCELED/UNKNOWN→失败，其余（含 null）保守视为运行中，交给超时兜底收敛。
     */
    private int mapStatus(String status) {
        if (status == null) {
            return GenerationStatusResult.STATUS_RUNNING;
        }
        return switch (status.toUpperCase()) {
            case "PENDING" -> GenerationStatusResult.STATUS_QUEUED;
            case "RUNNING", "SUSPENDED" -> GenerationStatusResult.STATUS_RUNNING;
            case "SUCCEEDED" -> GenerationStatusResult.STATUS_SUCCEEDED;
            case "FAILED", "CANCELED", "CANCELLED", "UNKNOWN" -> GenerationStatusResult.STATUS_FAILED;
            default -> GenerationStatusResult.STATUS_RUNNING;
        };
    }

    private GenerationStatusResult running(String msg) {
        return GenerationStatusResult.builder()
                .status(GenerationStatusResult.STATUS_RUNNING)
                .errorMsg(msg)
                .build();
    }
}
