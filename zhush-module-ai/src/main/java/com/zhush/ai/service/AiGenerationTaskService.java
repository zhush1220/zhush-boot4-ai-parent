package com.zhush.ai.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.zhush.ai.entity.AiChatMessageEntity;
import com.zhush.ai.entity.AiGenerationTaskEntity;
import com.zhush.ai.enums.AiTaskType;
import com.zhush.ai.generation.FileArtifact;
import com.zhush.ai.generation.RemoteFileFetcher;
import com.zhush.ai.generation.provider.GenerationProvider;
import com.zhush.ai.generation.provider.GenerationProviderRegistry;
import com.zhush.ai.generation.provider.GenerationRequest;
import com.zhush.ai.generation.provider.GenerationStatusResult;
import com.zhush.ai.generation.provider.GenerationSubmitResult;
import com.zhush.ai.mapper.AiChatMessageMapper;
import com.zhush.ai.mapper.AiGenerationTaskMapper;
import com.zhush.agentscope.config.AgentScopeProperties;
import com.zhush.agentscope.context.AgentContext;
import com.zhush.agentscope.model.AgentModelResolver;
import com.zhush.agentscope.model.ModelConfig;
import com.zhush.common.exception.ServiceExceptionUtil;
import com.zhush.file.core.FileRecordStore;
import com.zhush.file.core.FileUploadResult;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.zhush.ai.enums.AiErrorCode.GENERATION_MODEL_UNAVAILABLE;
import static com.zhush.ai.enums.AiErrorCode.GENERATION_SUBMIT_FAIL;

/**
 * 生成任务服务：图像/视频「提交 → 轮询 → 转存 → 回填」异步状态机的编排层。
 *
 * <p>与 {@code GenerationProvider}（纯供应商交互）分工：Provider 只发 SDK 请求、归一化状态；
 * 本服务负责凭证解析、任务落库、线程池轮询、产物转存 {@code infra_file}、越权隔离与配额计数。
 *
 * <p>产物 URL 约 24h 过期，故成功后必须转存，前端与历史回显一律用转存后的稳定外链。
 * 转存失败仍计一次生成配额（供应商额度已消耗，见方案 R3），任务置失败。
 *
 * <p>{@link ThreadPoolTaskExecutor} 承载「提交即轮询」；另有 {@link ScheduledExecutorService}
 * 周期兜底扫描悬挂任务（前端不轮询、实例重启都能收敛）。生产禁 Mock：无启用态生成模型时
 * {@link #submit} 直接抛 {@code GENERATION_MODEL_UNAVAILABLE}，不落脏任务。
 *
 * @author zhush
 */
@Service
public class AiGenerationTaskService {

    private static final Logger log = LoggerFactory.getLogger(AiGenerationTaskService.class);

    private static final int SWEEP_BATCH = 20;

    private final AiGenerationTaskMapper taskMapper;
    private final AiChatMessageMapper messageMapper;
    private final GenerationProviderRegistry providerRegistry;
    private final AgentModelResolver modelResolver;
    private final RemoteFileFetcher remoteFileFetcher;
    private final FileRecordStore fileRecordStore;
    private final AiQuotaService quotaService;
    private final AgentScopeProperties properties;
    private final ObjectMapper objectMapper;

    private ThreadPoolTaskExecutor workers;
    private ScheduledExecutorService sweeper;

    public AiGenerationTaskService(AiGenerationTaskMapper taskMapper,
                                   AiChatMessageMapper messageMapper,
                                   GenerationProviderRegistry providerRegistry,
                                   AgentModelResolver modelResolver,
                                   RemoteFileFetcher remoteFileFetcher,
                                   FileRecordStore fileRecordStore,
                                   AiQuotaService quotaService,
                                   AgentScopeProperties properties,
                                   ObjectMapper objectMapper) {
        this.taskMapper = taskMapper;
        this.messageMapper = messageMapper;
        this.providerRegistry = providerRegistry;
        this.modelResolver = modelResolver;
        this.remoteFileFetcher = remoteFileFetcher;
        this.fileRecordStore = fileRecordStore;
        this.quotaService = quotaService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        AgentScopeProperties.Generation gen = properties.getGeneration();
        workers = new ThreadPoolTaskExecutor();
        workers.setCorePoolSize(Math.max(1, gen.getThreads()));
        workers.setMaxPoolSize(Math.max(1, gen.getThreads()));
        workers.setQueueCapacity(Math.max(1, gen.getQueueCapacity()));
        workers.setThreadNamePrefix("ai-generation-");
        workers.initialize();

        sweeper = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "ai-generation-sweep");
            t.setDaemon(true);
            return t;
        });
        int period = Math.max(5, gen.getSweepIntervalSeconds());
        sweeper.scheduleWithFixedDelay(this::sweepSafely, period, period, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy() {
        if (workers != null) {
            workers.shutdown();
        }
        if (sweeper != null) {
            sweeper.shutdownNow();
        }
    }

    // ================ 提交 ================

    /**
     * 提交一次生成任务：解析凭证 → 落库 → 供应商提交 → 转入线程池轮询，立即返回对外任务号。
     *
     * <p>调用方（工具层）已完成限流/合规/配额前置校验，本方法只负责编排与落库。
     *
     * @return 生成的任务实体（含 {@code taskId}）
     */
    public AiGenerationTaskEntity submit(AiTaskType type, String prompt, String size,
                                         Integer n, Integer duration, AgentContext ctx) {
        ModelConfig model = modelResolver.resolveByCapability(type.capability())
                .orElseThrow(() -> ServiceExceptionUtil.exception(GENERATION_MODEL_UNAVAILABLE));
        GenerationProvider provider = providerRegistry.get(model.getType());

        String taskId = UUID.randomUUID().toString().replace("-", "");
        AiGenerationTaskEntity entity = new AiGenerationTaskEntity();
        entity.setTaskId(taskId);
        entity.setRemoteTaskId("");
        entity.setProvider(provider.name());
        entity.setTaskType(type.getCode());
        entity.setModel(model.getModel());
        entity.setPrompt(prompt);
        entity.setOptions(buildOptions(size, n, duration));
        entity.setSessionId(ctx == null || ctx.getSessionId() == null ? "" : ctx.getSessionId());
        entity.setMessageId(0L);
        entity.setUserId(ctx == null ? 0L : ctx.getUserId());
        entity.setUserType(ctx == null ? "admin" : ctx.getUserType());
        entity.setTenantId(0L);
        entity.setStatus(AiGenerationTaskEntity.STATUS_QUEUED);
        entity.setFileId(0L);
        entity.setResultUrl("");
        entity.setFileSize(0L);
        entity.setErrorMsg("");
        entity.setPollCount(0);
        entity.setSubmitTime(LocalDateTime.now());
        taskMapper.insert(entity);

        GenerationRequest request = GenerationRequest.builder()
                .type(type)
                .provider(provider.name())
                .model(model.getModel())
                .apiKey(model.getApiKey())
                .prompt(prompt)
                .size(resolveSize(type, size))
                .n(type == AiTaskType.IMAGE ? resolveCount(n) : null)
                .duration(type == AiTaskType.VIDEO ? resolveDuration(duration) : null)
                .build();

        GenerationSubmitResult submitted = provider.submit(request);
        if (!submitted.isSuccess()) {
            markFailed(entity.getId(), submitted.getErrorCode(), submitted.getErrorMsg());
            throw ServiceExceptionUtil.exception(GENERATION_SUBMIT_FAIL,
                    submitted.getErrorMsg() == null ? "供应商未接受任务" : submitted.getErrorMsg());
        }
        // 提交成功即计一次生成配额（R3：即便后续转存失败，供应商额度已消耗）
        quotaService.recordGeneration(entity.getUserId(), entity.getUserType(), type);

        UpdateChain.create(taskMapper)
                .set(AiGenerationTaskEntity::getRemoteTaskId, submitted.getRemoteTaskId())
                .set(AiGenerationTaskEntity::getStatus, AiGenerationTaskEntity.STATUS_RUNNING)
                .where(AiGenerationTaskEntity::getId).eq(entity.getId())
                .update();
        entity.setStatus(AiGenerationTaskEntity.STATUS_RUNNING);

        try {
            workers.execute(() -> pollAndFinalize(entity.getTaskId()));
        } catch (RejectedExecutionException rex) {
            // 队列满：保持 RUNNING 状态，交由兜底扫描接管（不阻塞、不失败）
            log.warn("生成任务线程池已满，转交兜底扫描接管, taskId={}", taskId);
        }
        return entity;
    }

    // ================ 轮询 / 收敛 ================

    /**
     * 线程内轮询直至终态或等待上限；上限内未收敛则释放线程，交由兜底扫描接管。
     */
    private void pollAndFinalize(String taskId) {
        AiGenerationTaskEntity entity = taskMapper.selectByTaskId(taskId);
        if (entity == null || isTerminal(entity.getStatus())) {
            return;
        }
        AiTaskType type = AiTaskType.of(entity.getTaskType());
        Optional<ModelConfig> modelOpt = modelResolver.resolveByCapability(type.capability());
        if (modelOpt.isEmpty()) {
            markFailed(entity.getId(), "NO_MODEL", "运行期未找到启用的生成模型");
            return;
        }
        GenerationProvider provider = providerRegistry.get(entity.getProvider());
        ModelConfig model = modelOpt.get();

        long pollInterval = Math.max(1, pollIntervalSeconds(type));
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(Math.max(1, maxWaitSeconds(type)));
        while (System.nanoTime() < deadlineNanos) {
            GenerationStatusResult status = provider.fetch(type, model.getApiKey(), entity.getRemoteTaskId());
            bumpPollCount(entity.getId());
            if (handleStatus(entity, type, status)) {
                return;
            }
            // 整体超时收敛：超过 timeout 仍未终态，置超时并停止等待
            if (isElapsed(entity.getSubmitTime(), timeoutSeconds(type))) {
                markTimeout(entity.getId());
                return;
            }
            sleep(pollInterval);
        }
        log.info("生成任务线程等待达上限，转交兜底扫描, taskId={}", taskId);
    }

    /**
     * 处理一次状态查询结果；返回 true 表示任务已进入终态（本方法已完成落库）。
     */
    private boolean handleStatus(AiGenerationTaskEntity entity, AiTaskType type, GenerationStatusResult status) {
        if (status.isSucceeded()) {
            return transferAndComplete(entity, type, status.getResultUrl());
        }
        if (status.isFailed()) {
            markFailed(entity.getId(), status.getErrorCode(), status.getErrorMsg());
            return true;
        }
        return false;
    }

    /**
     * 下载供应商产物并转存到文件服务，成功则回填消息附件；转存失败置任务失败（配额仍已计，见 R3）。
     */
    private boolean transferAndComplete(AiGenerationTaskEntity entity, AiTaskType type, String providerUrl) {
        if (providerUrl == null || providerUrl.isBlank()) {
            markFailed(entity.getId(), "EMPTY_RESULT", "供应商返回产物地址为空");
            return true;
        }
        try {
            byte[] bytes = remoteFileFetcher.fetchFromAllowedHosts(providerUrl);
            String format = type == AiTaskType.VIDEO ? "mp4" : "png";
            String contentType = type == AiTaskType.VIDEO ? "video/mp4" : "image/png";
            String fileName = (type == AiTaskType.VIDEO ? "video-" : "image-") + entity.getTaskId() + "." + format;
            FileUploadResult uploaded = fileRecordStore.upload(bytes, fileName, contentType,
                    entity.getUserType(), entity.getUserId());

            FileArtifact artifact = FileArtifact.builder()
                    .type(type == AiTaskType.VIDEO ? "video" : "image")
                    .format(format)
                    .name(fileName)
                    .url(uploaded.getUrl())
                    .size(uploaded.getSize() == null ? (long) bytes.length : uploaded.getSize())
                    .fileId(uploaded.getId())
                    .build();

            // 条件更新（仅当仍未终态）：避免线程轮询与兜底扫描双写
            boolean won = UpdateChain.create(taskMapper)
                    .set(AiGenerationTaskEntity::getStatus, AiGenerationTaskEntity.STATUS_SUCCEEDED)
                    .set(AiGenerationTaskEntity::getResultUrl, uploaded.getUrl())
                    .set(AiGenerationTaskEntity::getFileId, uploaded.getId() == null ? 0L : uploaded.getId())
                    .set(AiGenerationTaskEntity::getFileSize, artifact.getSize())
                    .set(AiGenerationTaskEntity::getFinishTime, LocalDateTime.now())
                    .where(AiGenerationTaskEntity::getId).eq(entity.getId())
                    .and(AiGenerationTaskEntity::getStatus).in(
                            AiGenerationTaskEntity.STATUS_QUEUED, AiGenerationTaskEntity.STATUS_RUNNING)
                    .update();
            if (won) {
                backfillMessage(entity, artifact);
            }
            return true;
        } catch (Exception e) {
            log.error("生成产物转存失败, taskId={}, url={}", entity.getTaskId(), providerUrl, e);
            markFailed(entity.getId(), "TRANSFER_FAIL", e.getMessage());
            return true;
        }
    }

    /**
     * 尽力回填：把产物追加到该会话最近一条助手消息的 attachments（历史回显同源）。
     * 无会话/无助手消息/序列化失败均静默跳过（不影响任务成功状态）。
     */
    private void backfillMessage(AiGenerationTaskEntity entity, FileArtifact artifact) {
        if (entity.getSessionId() == null || entity.getSessionId().isBlank()) {
            return;
        }
        try {
            List<AiChatMessageEntity> messages = messageMapper.selectBySessionId(entity.getSessionId());
            AiChatMessageEntity last = null;
            for (AiChatMessageEntity m : messages) {
                if ("assistant".equals(m.getRole())) {
                    last = m;
                }
            }
            if (last == null) {
                return;
            }
            String json = objectMapper.writeValueAsString(artifact);
            String existing = last.getAttachments();
            String merged = (existing == null || existing.isBlank() || "[]".equals(existing.trim()))
                    ? "[" + json + "]"
                    : existing.trim().substring(0, existing.trim().length() - 1) + "," + json + "]";
            AiChatMessageEntity patch = new AiChatMessageEntity();
            patch.setId(last.getId());
            patch.setAttachments(merged);
            messageMapper.update(patch);
        } catch (Exception e) {
            log.warn("生成产物回填消息失败, taskId={}, sessionId={}, error={}",
                    entity.getTaskId(), entity.getSessionId(), e.getMessage());
        }
    }

    // ================ 兜底扫描 ================

    private void sweepSafely() {
        try {
            sweep();
        } catch (Exception e) {
            log.warn("生成任务兜底扫描异常: {}", e.getMessage());
        }
    }

    /**
     * 扫描悬挂任务（排队/运行且提交已久）：主动查询一次并收敛，超整体超时者置超时。
     */
    private void sweep() {
        AgentScopeProperties.Generation gen = properties.getGeneration();
        int stale = Math.max(gen.getImage().getStaleSeconds(), gen.getVideo().getStaleSeconds());
        LocalDateTime before = LocalDateTime.now().minusSeconds(stale);
        List<AiGenerationTaskEntity> pending = taskMapper.selectStaleUnfinished(before, SWEEP_BATCH);
        for (AiGenerationTaskEntity entity : pending) {
            convergeOne(entity);
        }
    }

    private void convergeOne(AiGenerationTaskEntity entity) {
        AiTaskType type = AiTaskType.of(entity.getTaskType());
        Optional<ModelConfig> modelOpt = modelResolver.resolveByCapability(type.capability());
        if (modelOpt.isEmpty()) {
            if (isElapsed(entity.getSubmitTime(), timeoutSeconds(type))) {
                markTimeout(entity.getId());
            }
            return;
        }
        GenerationProvider provider = providerRegistry.get(entity.getProvider());
        GenerationStatusResult status = provider.fetch(type, modelOpt.get().getApiKey(), entity.getRemoteTaskId());
        bumpPollCount(entity.getId());
        if (handleStatus(entity, type, status)) {
            return;
        }
        if (isElapsed(entity.getSubmitTime(), timeoutSeconds(type))) {
            markTimeout(entity.getId());
        } else {
            // 仍运行中且线程可能已丢失：重新尝试拉起轮询（幂等，条件更新防止与在跑线程重复）
            try {
                workers.execute(() -> pollAndFinalize(entity.getTaskId()));
            } catch (RejectedExecutionException ignored) {
                // 队列仍满则下一轮扫描再处理
            }
        }
    }

    // ================ 状态落库 ================

    private void markFailed(Long id, String code, String msg) {
        UpdateChain.create(taskMapper)
                .set(AiGenerationTaskEntity::getStatus, AiGenerationTaskEntity.STATUS_FAILED)
                .set(AiGenerationTaskEntity::getErrorMsg, truncate(buildErr(code, msg)))
                .set(AiGenerationTaskEntity::getFinishTime, LocalDateTime.now())
                .where(AiGenerationTaskEntity::getId).eq(id)
                .and(AiGenerationTaskEntity::getStatus).in(
                        AiGenerationTaskEntity.STATUS_QUEUED, AiGenerationTaskEntity.STATUS_RUNNING)
                .update();
    }

    private void markTimeout(Long id) {
        UpdateChain.create(taskMapper)
                .set(AiGenerationTaskEntity::getStatus, AiGenerationTaskEntity.STATUS_TIMEOUT)
                .set(AiGenerationTaskEntity::getErrorMsg, "生成超时，请重试")
                .set(AiGenerationTaskEntity::getFinishTime, LocalDateTime.now())
                .where(AiGenerationTaskEntity::getId).eq(id)
                .and(AiGenerationTaskEntity::getStatus).in(
                        AiGenerationTaskEntity.STATUS_QUEUED, AiGenerationTaskEntity.STATUS_RUNNING)
                .update();
    }

    private void bumpPollCount(Long id) {
        AiGenerationTaskEntity cur = taskMapper.selectOneByQuery(
                QueryWrapper.create().select(AiGenerationTaskEntity::getPollCount)
                        .where(AiGenerationTaskEntity::getId).eq(id));
        int next = (cur == null || cur.getPollCount() == null ? 0 : cur.getPollCount()) + 1;
        UpdateChain.create(taskMapper)
                .set(AiGenerationTaskEntity::getPollCount, next)
                .where(AiGenerationTaskEntity::getId).eq(id)
                .update();
    }

    // ================ 参数解析（按类型夹紧） ================

    private String resolveSize(AiTaskType type, String size) {
        if (size != null && !size.isBlank()) {
            return size.trim();
        }
        AgentScopeProperties.Generation gen = properties.getGeneration();
        return type == AiTaskType.VIDEO ? gen.getVideo().getDefaultSize() : gen.getImage().getDefaultSize();
    }

    private Integer resolveCount(Integer n) {
        AgentScopeProperties.Image image = properties.getGeneration().getImage();
        int max = Math.max(1, image.getMaxCount());
        if (n == null || n <= 0) {
            return Math.min(Math.max(1, image.getDefaultCount()), max);
        }
        return Math.min(n, max);
    }

    private Integer resolveDuration(Integer duration) {
        AgentScopeProperties.Video video = properties.getGeneration().getVideo();
        int min = Math.max(1, video.getMinDuration());
        int max = Math.max(min, video.getMaxDuration());
        if (duration == null || duration <= 0) {
            return Math.min(Math.max(video.getDefaultDuration(), min), max);
        }
        return Math.min(Math.max(duration, min), max);
    }

    private long pollIntervalSeconds(AiTaskType type) {
        AgentScopeProperties.Generation gen = properties.getGeneration();
        return type == AiTaskType.VIDEO ? gen.getVideo().getPollIntervalSeconds() : gen.getImage().getPollIntervalSeconds();
    }

    private long maxWaitSeconds(AiTaskType type) {
        AgentScopeProperties.Generation gen = properties.getGeneration();
        return type == AiTaskType.VIDEO ? gen.getVideo().getMaxWaitSeconds() : gen.getImage().getMaxWaitSeconds();
    }

    private long timeoutSeconds(AiTaskType type) {
        AgentScopeProperties.Generation gen = properties.getGeneration();
        int minutes = type == AiTaskType.VIDEO ? gen.getVideo().getTimeoutMinutes() : gen.getImage().getTimeoutMinutes();
        return Duration.ofMinutes(Math.max(1, minutes)).getSeconds();
    }

    // ================ 通用小工具 ================

    private String buildOptions(String size, Integer n, Integer duration) {
        Map<String, Object> options = new LinkedHashMap<>();
        if (size != null && !size.isBlank()) {
            options.put("size", size);
        }
        if (n != null) {
            options.put("n", n);
        }
        if (duration != null) {
            options.put("duration", duration);
        }
        try {
            return objectMapper.writeValueAsString(options);
        } catch (Exception e) {
            return "{}";
        }
    }

    private boolean isTerminal(Integer status) {
        if (status == null) {
            return false;
        }
        return status == AiGenerationTaskEntity.STATUS_SUCCEEDED
                || status == AiGenerationTaskEntity.STATUS_FAILED
                || status == AiGenerationTaskEntity.STATUS_TIMEOUT;
    }

    private boolean isElapsed(LocalDateTime since, long seconds) {
        return since != null && since.plusSeconds(seconds).isBefore(LocalDateTime.now());
    }

    private String buildErr(String code, String msg) {
        String base = msg == null || msg.isBlank() ? "生成失败" : msg;
        return code == null || code.isBlank() ? base : code + ": " + base;
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 480 ? text.substring(0, 480) : text;
    }

    private void sleep(long seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
