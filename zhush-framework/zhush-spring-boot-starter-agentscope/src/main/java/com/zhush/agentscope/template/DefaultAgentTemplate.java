package com.zhush.agentscope.template;

import tools.jackson.databind.ObjectMapper;
import com.zhush.agentscope.agent.AgentDefinition;
import com.zhush.agentscope.agent.AgentRegistry;
import com.zhush.agentscope.agent.AgentReply;
import com.zhush.agentscope.config.AgentScopeProperties;
import com.zhush.agentscope.context.AgentContext;
import com.zhush.agentscope.context.SessionContextResolver;
import com.zhush.agentscope.event.AgentApprovalRequiredEvent;
import com.zhush.agentscope.event.AgentErrorEvent;
import com.zhush.agentscope.event.AgentEvent;
import com.zhush.agentscope.event.AgentEventPublisher;
import com.zhush.agentscope.event.AgentEventType;
import com.zhush.agentscope.event.AgentFinishedEvent;
import com.zhush.agentscope.event.AgentResumeChannels;
import com.zhush.agentscope.event.AgentStartedEvent;
import com.zhush.agentscope.event.AgentTextDeltaEvent;
import com.zhush.agentscope.event.AgentToolCalledEvent;
import com.zhush.agentscope.event.AgentToolResultEvent;
import com.zhush.agentscope.model.AgentModelResolver;
import com.zhush.agentscope.model.DefaultModelFactory;
import com.zhush.agentscope.safety.ApprovalRequest;
import com.zhush.agentscope.safety.ApprovalPersister;
import com.zhush.agentscope.safety.SensitiveWordMatch;
import com.zhush.agentscope.safety.SensitiveWordResolver;
import com.zhush.agentscope.safety.ToolApprovalHandler;
import com.zhush.agentscope.session.AgentSessionManager;
import com.zhush.agentscope.session.SessionMessage;
import com.zhush.agentscope.token.TokenUsageAccumulator;
import com.zhush.agentscope.tool.ReflectiveAgentTool;
import com.zhush.agentscope.tool.RegisteredTool;
import com.zhush.agentscope.tool.ToolkitRegistry;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.ConfirmResult;
import io.agentscope.core.event.ModelCallEndEvent;
import io.agentscope.core.event.RequireUserConfirmEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.event.ToolResultEndEvent;
import io.agentscope.core.event.ToolResultTextDeltaEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolResultState;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.model.Model;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionMode;
import io.agentscope.core.permission.PermissionRule;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.tool.ToolCallParam;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.harness.agent.HarnessAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * DefaultAgentTemplate：业务侧唯一门面，负责把 {@link AgentDefinition} + 工具 + 会话历史
 * 组装成 AgentScope {@link HarnessAgent} 并驱动调用，产出统一的事件/回复模型。
 *
 * <p>适配点：① 用 {@link ReflectiveAgentTool} 把业务 {@code @AgentTool} 桥接为 AgentScope 原生工具；
 * ② 会话上下文装配按来源位双轨：旧会话由 {@link AgentSessionManager} 每轮 {@code toMsgs} 全量重发历史；
 * 原生会话（{@code nativeState.enabled} 且状态存储可用）以 AgentScope {@code AgentState}（Redis）为运行态
 * 唯一事实源，仅发本轮增量，由框架恢复历史；③ 敏感工具审批：旧路径走 {@link ToolApprovalHandler} 工具层挂起，
 * 原生路径可选（{@code safety.nativeConfirm}）下沉到权限层 REQUIRE_USER_CONFIRM。
 *
 * @author zhush
 */
public class DefaultAgentTemplate implements AgentTemplate {

    private static final Logger log = LoggerFactory.getLogger(DefaultAgentTemplate.class);

    private final AgentRegistry agentRegistry;
    private final ToolkitRegistry toolkitRegistry;
    private final DefaultModelFactory modelFactory;
    private final AgentSessionManager sessionManager;
    private final AgentEventPublisher eventPublisher;
    private final SessionContextResolver contextResolver;
    private final ToolApprovalHandler approvalHandler;
    private final AgentScopeProperties properties;
    private final ObjectMapper objectMapper;
    private final AgentModelResolver modelResolver;
    private final SensitiveWordResolver sensitiveWordResolver;
    /** 审批落库 SPI（可为 null：业务未提供实现时仅 Redis 状态、DB 不可见） */
    private final ApprovalPersister approvalPersister;
    /** AgentScope 原生状态存储（可为 null：仅 nativeState.enabled 且 Redisson 可用时装配） */
    private final AgentStateStore agentStateStore;
    /** 续跑共享通道（可为 null：业务未启用 SSE 续跑通道时不推送，仅落库供轮询） */
    private final AgentResumeChannels resumeChannels;

    private final ConcurrentHashMap<String, HarnessAgent> harnessCache = new ConcurrentHashMap<>();

    /**
     * 待确认缓存：{@code sessionId -> (replyId -> 待确认工具调用)}，供 {@link #resumeConfirm} 重建
     * {@code ConfirmResult}（携带原始 {@link ToolUseBlock} id 以匹配框架持久化的挂起态）。
     *
     * <p>单实例内存缓存，重启/多实例丢失；路线 B 生产化需将挂起工具调用一并存入 Redis state。
     */
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, PendingConfirm>> pendingConfirms =
            new ConcurrentHashMap<>();

    /** 挂起待确认快照：运行态 userId（用于按同一 state key 恢复）+ 本轮待确认工具调用列表 */
    private record PendingConfirm(String userId, List<ToolUseBlock> toolCalls) {
    }

    public DefaultAgentTemplate(AgentRegistry agentRegistry, ToolkitRegistry toolkitRegistry,
                                DefaultModelFactory modelFactory, AgentSessionManager sessionManager,
                                AgentEventPublisher eventPublisher, SessionContextResolver contextResolver,
                                ToolApprovalHandler approvalHandler, AgentScopeProperties properties,
                                ObjectMapper objectMapper, AgentModelResolver modelResolver,
                                SensitiveWordResolver sensitiveWordResolver, ApprovalPersister approvalPersister,
                                AgentStateStore agentStateStore, AgentResumeChannels resumeChannels) {
        this.agentRegistry = agentRegistry;
        this.toolkitRegistry = toolkitRegistry;
        this.modelFactory = modelFactory;
        this.sessionManager = sessionManager;
        this.eventPublisher = eventPublisher;
        this.contextResolver = contextResolver;
        this.approvalHandler = approvalHandler;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.modelResolver = modelResolver;
        this.sensitiveWordResolver = sensitiveWordResolver;
        this.approvalPersister = approvalPersister;
        this.agentStateStore = agentStateStore;
        this.resumeChannels = resumeChannels;
    }

    @Override
    public AgentReply chat(String agentName, String sessionId, String userText) {
        AgentDefinition def = requireDefinition(agentName);
        String sid = sessionId != null ? sessionId : sessionManager.newSession(agentName, properties.getNativeState().isEnabled());
        boolean nativeState = sessionManager.isNativeState(sid);
        AgentContext ctx = contextResolver.resolve();
        sessionManager.appendMessage(sid, userMessage(userText));
        HarnessAgent agent = buildHarness(def, nativeState);
        RuntimeContext rc = runtimeContext(def, sid, ctx);
        // 原生会话只发本轮增量（历史由框架从 AgentState 恢复），旧会话仍全量重发 toMsgs
        List<Msg> input = nativeState ? List.of(userMsg(userText)) : toMsgs(sessionManager.loadHistory(sid));
        long start = System.currentTimeMillis();
        String model = resolveModelCode(def);
        try {
            Msg reply = agent.call(input, rc).block(properties.getRequestTimeout());
            String text = reply == null ? "" : reply.getTextContent();
            sessionManager.appendMessage(sid, assistantMessage(text));
            // 同步调用无事件流，用量直接取最终 Msg 携带的 usage
            TokenUsageAccumulator usage = new TokenUsageAccumulator();
            usage.accumulate(reply == null ? null : reply.getUsage());
            publish(finishEvent(def, sid, ctx, text, start, usage, model));
            return reply(def, sid, text, start, true, usage, model);
        } catch (Throwable t) {
            log.error("Agent 同步调用异常, agent={}, sessionId={}", agentName, sid, t);
            publish(errorEvent(def, sid, ctx, t.getMessage()));
            return reply(def, sid, t.getMessage(), start, false, null, model);
        }
    }

    @Override
    public Flux<AgentEvent> stream(String agentName, String sessionId, String userText) {
        AgentDefinition def = requireDefinition(agentName);
        String sid = sessionId != null ? sessionId : sessionManager.newSession(agentName, properties.getNativeState().isEnabled());
        boolean nativeState = sessionManager.isNativeState(sid);
        AgentContext ctx = contextResolver.resolve();
        sessionManager.appendMessage(sid, userMessage(userText));
        HarnessAgent agent = buildHarness(def, nativeState);
        RuntimeContext rc = runtimeContext(def, sid, ctx);
        // 原生会话只发本轮增量（历史由框架从 AgentState 恢复），旧会话仍全量重发 toMsgs
        List<Msg> input = nativeState ? List.of(userMsg(userText)) : toMsgs(sessionManager.loadHistory(sid));
        long start = System.currentTimeMillis();

        StringBuilder acc = new StringBuilder();
        boolean[] finished = {false};
        // 用量累加器与文本累加器同属一次调用的闭包状态，两条 finish 出口均需读取，漏一处即恒为 0
        TokenUsageAccumulator usage = new TokenUsageAccumulator();
        String model = resolveModelCode(def);
        // 工具结果文本聚合缓冲：toolCallId -> 增量，TOOL_RESULT_END 时一次性下发
        Map<String, StringBuilder> toolResults = new ConcurrentHashMap<>();

        Flux<AgentEvent> base = agent.streamEvents(input, rc)
                .mapNotNull(ev -> mapEvent(def, sid, ctx, ev, acc, finished, start, usage, model, toolResults))
                .doOnNext(e -> eventPublisher.publish(e));

        return base
                .concatWith(Mono.defer(() -> finished[0]
                        ? Mono.empty()
                        : Mono.just(finishEvent(def, sid, ctx, acc.toString(), start, usage, model))))
                .doOnNext(e -> eventPublisher.publish(e))
                .doOnComplete(() -> {
                    String text = acc.toString();
                    if (!text.isEmpty()) {
                        sessionManager.appendMessage(sid, assistantMessage(text));
                    }
                })
                .doOnError(t -> {
                    log.error("Agent 流式调用异常, agent={}, sessionId={}", agentName, sid, t);
                    publish(errorEvent(def, sid, ctx, t.getMessage()));
                });
    }

    @Override
    public CompletableFuture<AgentReply> chatAsync(String agentName, String sessionId, String userText) {
        return CompletableFuture.supplyAsync(() -> chat(agentName, sessionId, userText));
    }

    @Override
    public AgentReply callOnce(String agentName, String userText) {
        AgentDefinition def = requireDefinition(agentName);
        AgentContext ctx = contextResolver.resolve();
        HarnessAgent agent = buildHarness(def, false);
        RuntimeContext rc = runtimeContext(def, null, ctx);
        Msg userMsg = Msg.builder().role(MsgRole.USER).textContent(userText).build();
        long start = System.currentTimeMillis();
        String model = resolveModelCode(def);
        try {
            Msg reply = agent.call(userMsg, rc).block(properties.getRequestTimeout());
            String text = reply == null ? "" : reply.getTextContent();
            TokenUsageAccumulator usage = new TokenUsageAccumulator();
            usage.accumulate(reply == null ? null : reply.getUsage());
            return reply(def, null, text, start, true, usage, model);
        } catch (Throwable t) {
            log.error("Agent 一次性调用异常, agent={}", agentName, t);
            return reply(def, null, t.getMessage(), start, false, null, model);
        }
    }

    @Override
    public Flux<AgentEvent> resumeConfirm(String agentName, String sessionId, String replyId, boolean confirmed,
                                          Map<String, Object> modifiedArgs) {
        AgentDefinition def = requireDefinition(agentName);
        AgentContext ctx = contextResolver.resolve();
        ConcurrentHashMap<String, PendingConfirm> byReply = sessionId == null ? null : pendingConfirms.get(sessionId);
        PendingConfirm pending = byReply == null ? null : byReply.get(replyId);
        if (pending == null || pending.toolCalls().isEmpty()) {
            String err = "续跑失败：未找到待确认工具调用（可能已续跑、已过期或跨实例/重启丢失）, sessionId="
                    + sessionId + ", replyId=" + replyId;
            log.warn(err);
            AgentEvent errEvt = errorEvent(def, sessionId, ctx, err);
            // 早退也须经续跑通道下发并回收，否则已订阅的 resume-stream 前端会永久挂起
            if (resumeChannels != null) {
                resumeChannels.emit(sessionId, errEvt);
                resumeChannels.complete(sessionId);
            }
            return Flux.just(errEvt);
        }
        // 重建 ConfirmResult：同意且改参时以新入参覆盖，保留原始 toolCall id 以匹配框架持久化的挂起态
        List<ConfirmResult> results = new ArrayList<>(pending.toolCalls().size());
        for (ToolUseBlock tc : pending.toolCalls()) {
            ToolUseBlock effective = (confirmed && modifiedArgs != null && !modifiedArgs.isEmpty())
                    ? new ToolUseBlock(tc.getId(), tc.getName(), modifiedArgs)
                    : tc;
            results.add(new ConfirmResult(confirmed, effective));
        }
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(Msg.METADATA_CONFIRM_RESULTS, results);
        Msg confirmMsg = Msg.builder().role(MsgRole.USER).metadata(metadata).build();

        HarnessAgent agent = buildHarness(def, true);
        // state key 必须与挂起时一致：用首轮运行态 userId + 同一 sessionId 恢复
        RuntimeContext rc = RuntimeContext.builder().sessionId(sessionId).userId(pending.userId()).build();
        long start = System.currentTimeMillis();
        String model = resolveModelCode(def);
        StringBuilder acc = new StringBuilder();
        boolean[] finished = {false};
        TokenUsageAccumulator usage = new TokenUsageAccumulator();
        Map<String, StringBuilder> toolResults = new ConcurrentHashMap<>();

        Flux<AgentEvent> base = agent.streamEvents(List.of(confirmMsg), rc)
                .mapNotNull(ev -> mapEvent(def, sessionId, ctx, ev, acc, finished, start, usage, model, toolResults))
                .doOnNext(e -> eventPublisher.publish(e));
        return base
                .concatWith(Mono.defer(() -> finished[0]
                        ? Mono.empty()
                        : Mono.just(finishEvent(def, sessionId, ctx, acc.toString(), start, usage, model))))
                .doOnNext(e -> {
                    if (resumeChannels != null) {
                        resumeChannels.emit(sessionId, e);
                    }
                })
                .doOnComplete(() -> {
                    String text = acc.toString();
                    if (!text.isEmpty()) {
                        sessionManager.appendMessage(sessionId, assistantMessage(text));
                    }
                    if (byReply != null) {
                        byReply.remove(replyId);
                    }
                })
                .doOnError(t -> {
                    log.error("续跑异常, agent={}, sessionId={}", agentName, sessionId, t);
                    AgentEvent errEvt = errorEvent(def, sessionId, ctx, t.getMessage());
                    publish(errEvt);
                    if (resumeChannels != null) {
                        resumeChannels.emit(sessionId, errEvt);
                    }
                })
                .doFinally(sig -> {
                    // 无论完成/异常/取消，均回收通道，避免 Sink 泄漏与前端 resume-stream 悬挂
                    if (resumeChannels != null) {
                        resumeChannels.complete(sessionId);
                    }
                });
    }

    @Override
    public String newSession(String agentName) {
        return sessionManager.newSession(agentName);
    }

    @Override
    public void clearSession(String sessionId) {
        sessionManager.clearSession(sessionId);
    }

    @Override
    public void evictAgentCache(String agentName) {
        if (agentName == null) {
            harnessCache.clear();
        } else {
            // 缓存键为 agentName#native / agentName#legacy，按前缀清理同一 agent 的两个变体
            harnessCache.keySet().removeIf(k -> k.equals(agentName)
                    || k.startsWith(agentName + "#"));
        }
    }

    @Override
    public void evictAllAgentCache() {
        harnessCache.clear();
    }

    // ---------------------------------------------------------------------
    // 内部：构建 HarnessAgent / RuntimeContext / 历史
    // ---------------------------------------------------------------------

    private AgentDefinition requireDefinition(String agentName) {
        AgentDefinition def = agentRegistry.get(agentName);
        if (def == null) {
            throw new IllegalArgumentException("智能体不存在或未启用：" + agentName);
        }
        return def;
    }

    private HarnessAgent buildHarness(AgentDefinition def, boolean nativeState) {
        // 会话级灰度：仅当本会话启用原生状态且状态存储可用时，才装配 AgentScope 原生持久化；
        // 否则回退旧版（无 stateStore，历史由业务侧 toMsgs 全量重发）。缓存键含来源标志，
        // 避免同一 agentName 的原生/旧版实例互相污染上下文装配方式。
        boolean nativeSession = nativeState && agentStateStore != null;
        String cacheKey = def.getName() + (nativeSession ? "#native" : "#legacy");
        return harnessCache.computeIfAbsent(cacheKey, k -> {
            // 原生会话 + nativeConfirm：确认门禁下沉到权限层，工具层不再 suspended（互斥，避免双挂起）
            boolean deferToPermission = nativeSession && properties.getSafety().isNativeConfirm();
            Toolkit toolkit = buildToolkit(def, deferToPermission);
            HarnessAgent.Builder builder = HarnessAgent.builder()
                    .name(def.getName())
                    .description(def.getDescription() == null ? "" : def.getDescription())
                    .sysPrompt(def.getSystemPrompt() == null ? "" : def.getSystemPrompt())
                    .model(resolveModel(def))
                    .toolkit(toolkit)
                    .maxIters(def.getMaxSteps() > 0 ? def.getMaxSteps() : 10);
            if (nativeSession) {
                // 路线 B：以 Redis 状态存储为运行态唯一事实源（覆盖默认本地 JSON 文件，替换非叠加），
                // 开启挂起工具恢复以支持审批确认后同 key 续跑
                builder.stateStore(agentStateStore).enablePendingToolRecovery(true);
                if (deferToPermission) {
                    builder.permissionContext(buildPermissionContext(def)).stopOnReject(true);
                }
            }
            return builder.build();
        });
    }

    /**
     * 为标记需审批的敏感工具生成权限层 ASK 规则（路线 B nativeConfirm）。
     *
     * <p>局限：ASK 规则按 {@code toolName} 静态匹配，无法表达运行期动态敏感词命中即升级审批，
     * 故 nativeConfirm 相对旧版工具层动态门存在能力回退，需真实环境验证后方可生产启用。
     */
    private PermissionContextState buildPermissionContext(AgentDefinition def) {
        PermissionContextState.Builder pb = PermissionContextState.builder().mode(PermissionMode.DEFAULT);
        for (RegisteredTool rt : toolkitRegistry.buildToolkit(def)) {
            if (rt.isRequiresApproval()) {
                pb.addAskRule(rt.getName(), new PermissionRule(
                        rt.getName(), null, PermissionBehavior.ASK, "zhush-sensitive-tool"));
            }
        }
        return pb.build();
    }

    private Toolkit buildToolkit(AgentDefinition def, boolean deferToPermission) {
        Toolkit toolkit = new Toolkit();
        for (RegisteredTool rt : toolkitRegistry.buildToolkit(def)) {
            toolkit.registerAgentTool(new ReflectiveAgentTool(rt, objectMapper, approvalGate(def, rt, deferToPermission)));
        }
        return toolkit;
    }

    private Model resolveModel(AgentDefinition def) {
        String model = def.getModel();
        if (model == null || model.isEmpty()) {
            model = modelResolver.resolveDefaultCode();
        }
        AgentScopeProperties.ModelProperties mp = modelResolver.resolve(model);
        if (mp != null) {
            mp.setType(mp.getType() == null ? "dashscope" : mp.getType());
            return modelFactory.create(mp);
        }
        // 引用模型不可用（未启用/未配置）→ 回退默认模型，避免裸解析硬编码失败
        return resolveFallback(model);
    }

    private Model resolveFallback(String preferredModel) {
        String defaultModel = modelResolver.resolveDefaultCode();
        if (defaultModel != null && !defaultModel.isEmpty() && !defaultModel.equals(preferredModel)) {
            AgentScopeProperties.ModelProperties mp = modelResolver.resolve(defaultModel);
            if (mp != null) {
                mp.setType(mp.getType() == null ? "dashscope" : mp.getType());
                log.info("模型 {} 不可用, 回退默认模型 {}", preferredModel, defaultModel);
                return modelFactory.create(mp);
            }
        }
        log.warn("模型未命中动态池与 yaml 配置, 尝试按 type:model 解析: {}", preferredModel);
        return modelFactory.resolve(preferredModel);
    }

    /**
     * 本次调用预期使用的模型编码（用量与成本的定价依据）。
     *
     * <p>与 {@link #resolveModel} 的选择链保持一致：定义未指定时取运行态默认模型；
     * 极端情况下（引用模型不可用而回退）可能与实际调用模型不一致，仅影响成本归属。
     */
    private String resolveModelCode(AgentDefinition def) {
        String model = def.getModel();
        if (model != null && !model.isEmpty()) {
            return model;
        }
        return modelResolver.resolveDefaultCode();
    }

    private RuntimeContext runtimeContext(AgentDefinition def, String sessionId, AgentContext ctx) {
        RuntimeContext rc = RuntimeContext.builder()
                .sessionId(sessionId)
                .userId(String.valueOf(ctx == null || ctx.getUserId() == null ? 0L : ctx.getUserId()))
                .build();
        // 业务上下文经属性袋透传给工具方法：ReflectiveAgentTool 据此注入 AgentContext 型形参（LLM 不可见）
        if (ctx != null) {
            // 回填当前会话 ID：生成类工具据此把异步任务绑定到来源会话（产物回填/越权校验）
            ctx.setSessionId(sessionId);
            rc.put(AgentContext.class, ctx);
        }
        return rc;
    }

    private List<Msg> toMsgs(List<SessionMessage> history) {
        List<Msg> msgs = new ArrayList<>(history.size());
        for (SessionMessage m : history) {
            MsgRole role = switch (m.getRole() == null ? "" : m.getRole()) {
                case "user" -> MsgRole.USER;
                case "assistant" -> MsgRole.ASSISTANT;
                default -> null; // 工具/系统消息暂不重建，避免破坏消息配对
            };
            if (role == null) {
                continue;
            }
            msgs.add(Msg.builder().role(role).textContent(m.getContent()).build());
        }
        return msgs;
    }

    private SessionMessage userMessage(String content) {
        return SessionMessage.builder().role("user").content(content).build();
    }

    /**
     * 本轮增量用户消息（原生会话专用：不重建历史，仅发送本条）。
     */
    private Msg userMsg(String content) {
        return Msg.builder().role(MsgRole.USER).textContent(content).build();
    }

    private SessionMessage assistantMessage(String content) {
        return SessionMessage.builder().role("assistant").content(content).build();
    }

    // ---------------------------------------------------------------------
    // 内部：事件映射
    // ---------------------------------------------------------------------

    /**
     * 工具结果截断（事件负载不宜过大，前端产物提取只需头部 JSON）。
     */
    private static String truncateText(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() > max ? text.substring(0, max) + "...(已截断)" : text;
    }

    private AgentEvent mapEvent(AgentDefinition def, String sessionId, AgentContext ctx,
                                io.agentscope.core.event.AgentEvent ev, StringBuilder acc, boolean[] finished,
                                long start, TokenUsageAccumulator usage, String model,
                                Map<String, StringBuilder> toolResults) {
        io.agentscope.core.event.AgentEventType type = ev.getType();
        if (type == io.agentscope.core.event.AgentEventType.MODEL_CALL_END) {
            // 每次模型调用结束回传 ChatUsage：累加而不产出前端事件（多步 ReAct 逐步累加）
            usage.accumulate(((ModelCallEndEvent) ev).getUsage());
            return null;
        }
        if (type == io.agentscope.core.event.AgentEventType.TEXT_BLOCK_DELTA) {
            String delta = ((TextBlockDeltaEvent) ev).getDelta();
            if (delta != null) {
                acc.append(delta);
            }
            return AgentTextDeltaEvent.builder()
                    .eventType(AgentEventType.TEXT_DELTA)
                    .agentName(def.getName())
                    .sessionId(sessionId)
                    .tenantId(ctx.getTenantId())
                    .userId(ctx.getUserId())
                    .time(Instant.now())
                    .delta(delta == null ? "" : delta)
                    .build();
        }
        if (type == io.agentscope.core.event.AgentEventType.TOOL_CALL_START) {
            String toolName = ((ToolCallStartEvent) ev).getToolCallName();
            RegisteredTool rt = toolkitRegistry.get(toolName);
            boolean sensitive = rt != null && rt.isRequiresApproval();
            return AgentToolCalledEvent.builder()
                    .eventType(AgentEventType.TOOL_CALLED)
                    .agentName(def.getName())
                    .sessionId(sessionId)
                    .tenantId(ctx.getTenantId())
                    .userId(ctx.getUserId())
                    .time(Instant.now())
                    .id(((ToolCallStartEvent) ev).getToolCallId())
                    .toolName(toolName)
                    .requiresApproval(sensitive)
                    .approved(!sensitive)
                    .build();
        }
        if (type == io.agentscope.core.event.AgentEventType.TOOL_RESULT_TEXT_DELTA) {
            // 工具结果增量：仅聚合不转发，避免长 JSON 拆帧推送
            ToolResultTextDeltaEvent deltaEvent = (ToolResultTextDeltaEvent) ev;
            if (deltaEvent.getToolCallId() != null && deltaEvent.getDelta() != null) {
                toolResults.computeIfAbsent(deltaEvent.getToolCallId(), k -> new StringBuilder())
                        .append(deltaEvent.getDelta());
            }
            return null;
        }
        if (type == io.agentscope.core.event.AgentEventType.TOOL_RESULT_END) {
            ToolResultEndEvent endEvent = (ToolResultEndEvent) ev;
            StringBuilder buffer = endEvent.getToolCallId() == null
                    ? null : toolResults.remove(endEvent.getToolCallId());
            return AgentToolResultEvent.builder()
                    .eventType(AgentEventType.TOOL_RESULT)
                    .agentName(def.getName())
                    .sessionId(sessionId)
                    .tenantId(ctx.getTenantId())
                    .userId(ctx.getUserId())
                    .time(Instant.now())
                    .id(endEvent.getToolCallId())
                    .name(endEvent.getToolCallName())
                    .result(truncateText(buffer == null ? "" : buffer.toString(), 4000))
                    .ok(endEvent.getState() == null || endEvent.getState() == ToolResultState.SUCCESS)
                    .build();
        }
        if (type == io.agentscope.core.event.AgentEventType.AGENT_END) {
            finished[0] = true;
            return finishEvent(def, sessionId, ctx, acc.toString(), start, usage, model);
        }
        if (type == io.agentscope.core.event.AgentEventType.EXCEED_MAX_ITERS) {
            finished[0] = true;
            return errorEvent(def, sessionId, ctx, "超过最大推理步数，已中断");
        }
        if (type == io.agentscope.core.event.AgentEventType.REQUIRE_USER_CONFIRM) {
            // 路线 B：权限层（nativeConfirm）执行前拦截，转成业务审批事件 + 缓存待确认工具调用
            handleRequireConfirm(def, sessionId, ctx, (RequireUserConfirmEvent) ev);
            return null;
        }
        return null;
    }

    /**
     * 处理原生权限层发出的 {@code REQUIRE_USER_CONFIRM}：落 {@code agent_approval}（携 replyId）、
     * 发 {@code APPROVAL_REQUIRED} 事件、缓存待确认工具调用以供 {@link #resumeConfirm} 同 key 续跑。
     */
    private void handleRequireConfirm(AgentDefinition def, String sessionId, AgentContext ctx, RequireUserConfirmEvent ev) {
        List<ToolUseBlock> toolCalls = ev.getToolCalls();
        if (toolCalls == null || toolCalls.isEmpty()) {
            return;
        }
        String userId = String.valueOf(ctx.getUserId() == null ? 0L : ctx.getUserId());
        pendingConfirms.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
                .put(ev.getReplyId(), new PendingConfirm(userId, List.copyOf(toolCalls)));
        for (ToolUseBlock tu : toolCalls) {
            ApprovalRequest request = ApprovalRequest.builder()
                    .approvalId(UUID.randomUUID().toString().replace("-", ""))
                    .sessionId(sessionId)
                    .agentName(def.getName())
                    .toolName(tu.getName())
                    .replyId(ev.getReplyId())
                    .arguments(tu.getInput())
                    .proposerId(ctx.getUserId())
                    .proposerType(ctx.getUserType())
                    .proposerName(ctx.getNickname())
                    .tenantId(ctx.getTenantId())
                    .expireTime(Instant.now().plus(properties.getSessionTtl()))
                    .build();
            persistApproval(request);
            publish(AgentApprovalRequiredEvent.builder()
                    .eventType(AgentEventType.APPROVAL_REQUIRED)
                    .agentName(def.getName())
                    .sessionId(sessionId)
                    .tenantId(ctx.getTenantId())
                    .userId(ctx.getUserId())
                    .time(Instant.now())
                    .approvalId(request.getApprovalId())
                    .toolName(tu.getName())
                    .arguments(toJson(tu.getInput()))
                    .replyId(ev.getReplyId())
                    .build());
            log.info("原生权限层触发待确认, approvalId={}, replyId={}, tool={}",
                    request.getApprovalId(), ev.getReplyId(), tu.getName());
        }
    }

    private AgentFinishedEvent finishEvent(AgentDefinition def, String sessionId, AgentContext ctx,
                                           String reply, long start, TokenUsageAccumulator usage, String model) {
        if (usage != null && usage.isEmpty()) {
            // 部分供应商（如本地 Ollama）流式下不回填 usage：记 0 不估算，仅告警提示口径缺失
            log.debug("未采集到模型 usage, agent={}, sessionId={}, model={}", def.getName(), sessionId, model);
        }
        int steps = usage == null ? 1 : Math.max(usage.getStepCount(), 1);
        int inputTokens = usage == null ? 0 : usage.getInputTokens();
        int outputTokens = usage == null ? 0 : usage.getOutputTokens();
        return AgentFinishedEvent.builder()
                .eventType(AgentEventType.FINISHED)
                .agentName(def.getName())
                .sessionId(sessionId)
                .tenantId(ctx.getTenantId())
                .userId(ctx.getUserId())
                .time(Instant.now())
                .reply(reply)
                .stepCount(steps)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .totalTokens(usage == null ? 0 : usage.getTotalTokens())
                .cachedTokens(usage == null ? 0 : usage.getCachedTokens())
                .model(model)
                .durationMillis(System.currentTimeMillis() - start)
                .build();
    }

    private AgentEvent errorEvent(AgentDefinition def, String sessionId, AgentContext ctx, String error) {
        return AgentErrorEvent.builder()
                .eventType(AgentEventType.ERROR)
                .agentName(def.getName())
                .sessionId(sessionId)
                .tenantId(ctx.getTenantId())
                .userId(ctx.getUserId())
                .time(Instant.now())
                .error(error)
                .build();
    }

    private AgentReply reply(AgentDefinition def, String sessionId, String text, long start, boolean finished,
                             TokenUsageAccumulator usage, String model) {
        return AgentReply.builder()
                .sessionId(sessionId)
                .agentName(def.getName())
                .reply(text)
                .finished(finished)
                .stepCount(usage == null ? 1 : Math.max(usage.getStepCount(), 1))
                .inputTokens(usage == null ? 0 : usage.getInputTokens())
                .outputTokens(usage == null ? 0 : usage.getOutputTokens())
                .totalTokens(usage == null ? 0 : usage.getTotalTokens())
                .model(model)
                .durationMillis(System.currentTimeMillis() - start)
                .build();
    }

    // ---------------------------------------------------------------------
    // 内部：工具审批门
    // ---------------------------------------------------------------------

    private Function<ToolCallParam, Optional<ToolResultBlock>> approvalGate(AgentDefinition def, RegisteredTool rt, boolean deferToPermission) {
        AgentScopeProperties.Safety safety = properties.getSafety();
        String toolName = rt.getName();
        // 动态分级检测在调用时执行（敏感词库可运行期变更），命中词取最高级别
        return p -> {
            String inputJson = toJson(p.getInput());
            SensitiveWordMatch match = sensitiveWordResolver.match(toolName, inputJson);
            if (match.isHit() && match.getLevel() == SensitiveWordMatch.LEVEL_BLOCK) {
                log.warn("敏感词命中(拦截级), 拒绝执行, tool={}, words={}", toolName, match.getWords());
                return Optional.of(ToolResultBlock.error(
                        "操作被拦截：检测到敏感内容" + match.getWords() + "，请联系管理员"));
            }
            if (match.isHit() && match.getLevel() == SensitiveWordMatch.LEVEL_NOTICE) {
                log.warn("敏感词命中(提示级), 放行留痕, tool={}, words={}", toolName, match.getWords());
                return Optional.empty();
            }
            boolean staticSensitive = rt.isRequiresApproval();
            boolean dynamicApproval = match.isHit() && match.getLevel() >= SensitiveWordMatch.LEVEL_APPROVAL;
            // 原生确认门禁：静态敏感工具由权限层 ASK 先行拦截，工具层不再挂起（避免双挂起）；
            // 运行期动态命中仍由本层兜底（权限层按 toolName 静态匹配无法表达）
            if (deferToPermission && staticSensitive && !dynamicApproval) {
                return Optional.empty();
            }
            if (!safety.isApprovalEnabled() || !(staticSensitive || dynamicApproval)) {
                return Optional.empty();
            }
            AgentContext ctx = contextResolver.resolve();
            String sessionId = p.getRuntimeContext() == null ? null : p.getRuntimeContext().getSessionId();
            ApprovalRequest request = ApprovalRequest.builder()
                    .approvalId(UUID.randomUUID().toString().replace("-", ""))
                    .sessionId(sessionId)
                    .agentName(def.getName())
                    .toolName(toolName)
                    .arguments(p.getInput())
                    .proposerId(ctx.getUserId())
                    .proposerType(ctx.getUserType())
                    .proposerName(ctx.getNickname())
                    .tenantId(ctx.getTenantId())
                    .expireTime(Instant.now().plus(properties.getSessionTtl()))
                    .build();
            com.zhush.agentscope.safety.ApprovalStatus status = approvalHandler.submit(request);
            if (status == com.zhush.agentscope.safety.ApprovalStatus.APPROVED) {
                // 审批处理器直接放行（如无 Redis 降级、或已自动审批）：恢复正常执行
                // 无 Redis 降级护栏：审批状态不可持久化时，按开关 fail-fast 或降级放行并留痕
                if (!approvalHandler.persistent()) {
                    if (safety.isFailOnApprovalStoreUnavailable()) {
                        throw new IllegalStateException("审批状态存储不可用（无 Redis）且 "
                                + "failOnApprovalStoreUnavailable=true，拒绝放行敏感工具: " + toolName);
                    }
                    log.warn("审批存储不可用（无 Redis），降级放行敏感工具并留痕, tool={}, sessionId={}, approvalId={}",
                            toolName, sessionId, request.getApprovalId());
                }
                return Optional.empty();
            }
            // 挂起等待人工审批：先落 agent_approval（P0），使管理端审批列表可见、续跑有据可依
            persistApproval(request);
            AgentApprovalRequiredEvent approvalEvent = AgentApprovalRequiredEvent.builder()
                    .eventType(AgentEventType.APPROVAL_REQUIRED)
                    .agentName(def.getName())
                    .sessionId(sessionId)
                    .tenantId(ctx.getTenantId())
                    .userId(ctx.getUserId())
                    .time(Instant.now())
                    .approvalId(request.getApprovalId())
                    .toolName(toolName)
                    .arguments(inputJson)
                    .build();
            publish(approvalEvent);
            log.info("敏感工具触发审批, approvalId={}, tool={}", request.getApprovalId(), toolName);
            return Optional.of(ToolResultBlock.suspended(p.getToolUseBlock()));
        };
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map == null ? Map.of() : map);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * 回调业务侧落库待审批记录（{@code agent_approval}）。审批落库失败不得阻断 SSE 主流程，
     * 故此处吞掉异常仅告警：此时 Redis 仍有 PENDING 状态，管理端可经 Redis fast-path 兜底。
     */
    private void persistApproval(ApprovalRequest request) {
        if (approvalPersister == null) {
            log.warn("未提供 ApprovalPersister 实现, 审批记录不落库, approvalId={}", request.getApprovalId());
            return;
        }
        try {
            approvalPersister.persist(request);
        } catch (Exception e) {
            log.error("审批记录落库失败, approvalId={}", request.getApprovalId(), e);
        }
    }

    private void publish(AgentEvent event) {
        eventPublisher.publish(event);
    }
}
