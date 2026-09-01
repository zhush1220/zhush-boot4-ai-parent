package com.zhush.agentscope.template;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.zhush.agentscope.event.AgentStartedEvent;
import com.zhush.agentscope.event.AgentTextDeltaEvent;
import com.zhush.agentscope.event.AgentToolCalledEvent;
import com.zhush.agentscope.model.DefaultModelFactory;
import com.zhush.agentscope.safety.ApprovalRequest;
import com.zhush.agentscope.safety.ToolApprovalHandler;
import com.zhush.agentscope.session.AgentSessionManager;
import com.zhush.agentscope.session.SessionMessage;
import com.zhush.agentscope.tool.ReflectiveAgentTool;
import com.zhush.agentscope.tool.RegisteredTool;
import com.zhush.agentscope.tool.ToolkitRegistry;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.ToolCallParam;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.harness.agent.HarnessAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
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
 * ② 会话多轮历史由 {@link AgentSessionManager} 自行管理（关闭 AgentScope 内部会话持久化）；③ 敏感工具走
 * {@link ToolApprovalHandler} 挂起审批。
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

    private final ConcurrentHashMap<String, HarnessAgent> harnessCache = new ConcurrentHashMap<>();

    public DefaultAgentTemplate(AgentRegistry agentRegistry, ToolkitRegistry toolkitRegistry,
                                DefaultModelFactory modelFactory, AgentSessionManager sessionManager,
                                AgentEventPublisher eventPublisher, SessionContextResolver contextResolver,
                                ToolApprovalHandler approvalHandler, AgentScopeProperties properties,
                                ObjectMapper objectMapper) {
        this.agentRegistry = agentRegistry;
        this.toolkitRegistry = toolkitRegistry;
        this.modelFactory = modelFactory;
        this.sessionManager = sessionManager;
        this.eventPublisher = eventPublisher;
        this.contextResolver = contextResolver;
        this.approvalHandler = approvalHandler;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentReply chat(String agentName, String sessionId, String userText) {
        AgentDefinition def = requireDefinition(agentName);
        String sid = sessionId != null ? sessionId : sessionManager.newSession(agentName);
        AgentContext ctx = contextResolver.resolve();
        sessionManager.appendMessage(sid, userMessage(userText));
        HarnessAgent agent = buildHarness(def);
        RuntimeContext rc = runtimeContext(def, sid, ctx);
        List<Msg> history = toMsgs(sessionManager.loadHistory(sid));
        long start = System.currentTimeMillis();
        try {
            Msg reply = agent.call(history, rc).block(properties.getRequestTimeout());
            String text = reply == null ? "" : reply.getTextContent();
            sessionManager.appendMessage(sid, assistantMessage(text));
            return reply(def, sid, text, start, true);
        } catch (Throwable t) {
            log.error("Agent 同步调用异常, agent={}, sessionId={}", agentName, sid, t);
            publish(errorEvent(def, sid, ctx, t.getMessage()));
            return reply(def, sid, t.getMessage(), start, false);
        }
    }

    @Override
    public Flux<AgentEvent> stream(String agentName, String sessionId, String userText) {
        AgentDefinition def = requireDefinition(agentName);
        String sid = sessionId != null ? sessionId : sessionManager.newSession(agentName);
        AgentContext ctx = contextResolver.resolve();
        sessionManager.appendMessage(sid, userMessage(userText));
        HarnessAgent agent = buildHarness(def);
        RuntimeContext rc = runtimeContext(def, sid, ctx);
        List<Msg> history = toMsgs(sessionManager.loadHistory(sid));
        long start = System.currentTimeMillis();

        StringBuilder acc = new StringBuilder();
        boolean[] finished = {false};

        Flux<AgentEvent> base = agent.streamEvents(history, rc)
                .mapNotNull(ev -> mapEvent(def, sid, ctx, ev, acc, finished, start))
                .doOnNext(e -> eventPublisher.publish(e));

        return base
                .concatWith(Mono.defer(() -> finished[0] ? Mono.empty() : Mono.just(finishEvent(def, sid, ctx, acc.toString(), start))))
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
        HarnessAgent agent = buildHarness(def);
        RuntimeContext rc = runtimeContext(def, null, ctx);
        Msg userMsg = Msg.builder().role(MsgRole.USER).textContent(userText).build();
        long start = System.currentTimeMillis();
        try {
            Msg reply = agent.call(userMsg, rc).block(properties.getRequestTimeout());
            String text = reply == null ? "" : reply.getTextContent();
            return reply(def, null, text, start, true);
        } catch (Throwable t) {
            log.error("Agent 一次性调用异常, agent={}", agentName, t);
            return reply(def, null, t.getMessage(), start, false);
        }
    }

    @Override
    public String newSession(String agentName) {
        return sessionManager.newSession(agentName);
    }

    @Override
    public void clearSession(String sessionId) {
        sessionManager.clearSession(sessionId);
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

    private HarnessAgent buildHarness(AgentDefinition def) {
        return harnessCache.computeIfAbsent(def.getName(), n -> {
            Toolkit toolkit = buildToolkit(def);
            HarnessAgent.Builder builder = HarnessAgent.builder()
                    .name(def.getName())
                    .description(def.getDescription() == null ? "" : def.getDescription())
                    .sysPrompt(def.getSystemPrompt() == null ? "" : def.getSystemPrompt())
                    .model(resolveModel(def))
                    .toolkit(toolkit)
                    .maxIters(def.getMaxSteps() > 0 ? def.getMaxSteps() : 10);
            return builder.build();
        });
    }

    private Toolkit buildToolkit(AgentDefinition def) {
        Toolkit toolkit = new Toolkit();
        for (RegisteredTool rt : toolkitRegistry.buildToolkit(def)) {
            toolkit.registerAgentTool(new ReflectiveAgentTool(rt, objectMapper, approvalGate(def, rt)));
        }
        return toolkit;
    }

    private Model resolveModel(AgentDefinition def) {
        String model = def.getModel();
        if (model == null || model.isEmpty()) {
            model = properties.getDefaultModel();
        }
        AgentScopeProperties.ModelProperties mp = properties.getModels().get(model);
        if (mp != null) {
            mp.setType(mp.getType() == null ? "dashscope" : mp.getType());
            return modelFactory.create(mp);
        }
        return modelFactory.resolve(model);
    }

    private RuntimeContext runtimeContext(AgentDefinition def, String sessionId, AgentContext ctx) {
        return RuntimeContext.builder()
                .sessionId(sessionId)
                .userId(String.valueOf(ctx.getUserId() == null ? 0L : ctx.getUserId()))
                .build();
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

    private SessionMessage assistantMessage(String content) {
        return SessionMessage.builder().role("assistant").content(content).build();
    }

    // ---------------------------------------------------------------------
    // 内部：事件映射
    // ---------------------------------------------------------------------

    private AgentEvent mapEvent(AgentDefinition def, String sessionId, AgentContext ctx,
                                io.agentscope.core.event.AgentEvent ev, StringBuilder acc,
                                boolean[] finished, long start) {
        io.agentscope.core.event.AgentEventType type = ev.getType();
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
                    .toolName(toolName)
                    .requiresApproval(sensitive)
                    .approved(!sensitive)
                    .build();
        }
        if (type == io.agentscope.core.event.AgentEventType.AGENT_END) {
            finished[0] = true;
            return finishEvent(def, sessionId, ctx, acc.toString(), start);
        }
        if (type == io.agentscope.core.event.AgentEventType.EXCEED_MAX_ITERS) {
            finished[0] = true;
            return errorEvent(def, sessionId, ctx, "超过最大推理步数，已中断");
        }
        return null;
    }

    private AgentFinishedEvent finishEvent(AgentDefinition def, String sessionId, AgentContext ctx,
                                           String reply, long start) {
        return AgentFinishedEvent.builder()
                .eventType(AgentEventType.FINISHED)
                .agentName(def.getName())
                .sessionId(sessionId)
                .tenantId(ctx.getTenantId())
                .userId(ctx.getUserId())
                .time(Instant.now())
                .reply(reply)
                .stepCount(1)
                .inputTokens(0)
                .outputTokens(0)
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

    private AgentReply reply(AgentDefinition def, String sessionId, String text, long start, boolean finished) {
        return AgentReply.builder()
                .sessionId(sessionId)
                .agentName(def.getName())
                .reply(text)
                .finished(finished)
                .durationMillis(System.currentTimeMillis() - start)
                .build();
    }

    // ---------------------------------------------------------------------
    // 内部：工具审批门
    // ---------------------------------------------------------------------

    private Function<ToolCallParam, Optional<ToolResultBlock>> approvalGate(AgentDefinition def, RegisteredTool rt) {
        AgentScopeProperties.Safety safety = properties.getSafety();
        String toolName = rt.getName();
        boolean keywordHit = safety.getSensitiveKeywords().stream()
                .anyMatch(k -> toolName.contains(k));
        if (!safety.isApprovalEnabled() || (!rt.isRequiresApproval() && !keywordHit)) {
            return p -> Optional.empty();
        }
        return p -> {
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
                return Optional.empty();
            }
            AgentApprovalRequiredEvent approvalEvent = AgentApprovalRequiredEvent.builder()
                    .eventType(AgentEventType.APPROVAL_REQUIRED)
                    .agentName(def.getName())
                    .sessionId(sessionId)
                    .tenantId(ctx.getTenantId())
                    .userId(ctx.getUserId())
                    .time(Instant.now())
                    .approvalId(request.getApprovalId())
                    .toolName(toolName)
                    .arguments(toJson(p.getInput()))
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

    private void publish(AgentEvent event) {
        eventPublisher.publish(event);
    }
}
