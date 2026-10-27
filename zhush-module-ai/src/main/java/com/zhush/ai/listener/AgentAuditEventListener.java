package com.zhush.ai.listener;

import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.ai.entity.AiChatSessionEntity;
import com.zhush.ai.entity.AgentAuditLogEntity;
import com.zhush.ai.mapper.AiChatSessionMapper;
import com.zhush.ai.mapper.AgentAuditLogMapper;
import com.zhush.agentscope.event.AgentApprovalRequiredEvent;
import com.zhush.agentscope.event.AgentErrorEvent;
import com.zhush.agentscope.event.AgentEvent;
import com.zhush.agentscope.event.AgentEventListener;
import com.zhush.agentscope.event.AgentEventType;
import com.zhush.agentscope.event.AgentFinishedEvent;
import com.zhush.agentscope.event.AgentStartedEvent;
import com.zhush.agentscope.event.AgentToolCalledEvent;
import com.zhush.agentscope.event.AgentToolResultEvent;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Locale;
import java.util.Set;

/**
 * 审计事件监听器：将会话生命周期事件落库到 {@code agent_audit_log}。
 *
 * <p>跳过高频的文本增量事件（{@link com.zhush.agentscope.event.AgentTextDeltaEvent}），保留关键事件留痕。
 *
 * <p>工具结果只按长度截断落库，唯一例外是 {@link #BODY_OMITTED_TOOLS}：其结果含外部网页原文，
 * 可能带版权或敏感内容，落库前摘除正文、仅保留元数据与规模（方案 6.5）。
 *
 * @author zhush
 */
@Component
public class AgentAuditEventListener implements AgentEventListener {

    private static final Logger log = LoggerFactory.getLogger(AgentAuditEventListener.class);

    /** 工具结果字符上限（原为硬编码 4000，抽出供正文摘除后复用同一口径） */
    private static final int RESULT_MAX_CHARS = 4000;

    /**
     * 结果含外部原文、不得整段落库的工具。
     *
     * <p>按工具名而非"所有工具"摘除：搜索摘要等回包本身就是给模型看的内容，摘掉等于丢审计。
     */
    private static final Set<String> BODY_OMITTED_TOOLS = Set.of("web_fetch");

    private final AgentAuditLogMapper auditLogMapper;
    private final AiChatSessionMapper sessionMapper;
    private final ObjectMapper objectMapper;

    public AgentAuditEventListener(AgentAuditLogMapper auditLogMapper, AiChatSessionMapper sessionMapper,
                                   ObjectMapper objectMapper) {
        this.auditLogMapper = auditLogMapper;
        this.sessionMapper = sessionMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onEvent(AgentEvent event) {
        if (event == null || event.getEventType() == AgentEventType.TEXT_DELTA) {
            return;
        }
        AgentAuditLogEntity entity = new AgentAuditLogEntity();
        if (event instanceof AgentStartedEvent) {
            entity.setEventType("STARTED");
        } else if (event instanceof AgentFinishedEvent finishedEvent) {
            entity.setEventType("FINISHED");
            entity.setDuration(safeInt(finishedEvent.getDurationMillis()));
            // 优先取供应商回传的总 Token（权威值），缺失时回退输入+输出
            int total = finishedEvent.getTotalTokens();
            entity.setTokenUsage(total > 0 ? total
                    : finishedEvent.getOutputTokens() + finishedEvent.getInputTokens());
            entity.setStepCount(finishedEvent.getStepCount());
        } else if (event instanceof AgentToolCalledEvent toolEvent) {
            entity.setEventType("TOOL_CALLED");
            entity.setToolName(toolEvent.getToolName());
            entity.setToolArgs(truncate(toolEvent.getArguments(), 4000));
            entity.setToolResult(auditResult(toolEvent.getToolName(), toolEvent.getResult()));
        } else if (event instanceof AgentToolResultEvent resultEvent) {
            entity.setEventType("TOOL_RESULT");
            entity.setToolName(resultEvent.getName());
            entity.setToolResult(auditResult(resultEvent.getName(), resultEvent.getResult()));
            entity.setResultCode(resultEvent.isOk() ? 0 : 1);
            entity.setResultMsg(resultEvent.isOk() ? "" : "工具执行失败");
        } else if (event instanceof AgentErrorEvent errorEvent) {
            entity.setEventType("ERROR");
            entity.setResultCode(1);
            entity.setResultMsg(truncate(errorEvent.getError(), 500));
        } else if (event instanceof AgentApprovalRequiredEvent approvalEvent) {
            entity.setEventType("APPROVAL");
            entity.setToolName(approvalEvent.getToolName());
            entity.setToolArgs(approvalEvent.getArguments());
            entity.setResultMsg("待人工审批");
        } else {
            return;
        }
        entity.setSessionId(event.getSessionId());
        entity.setAgentName(event.getAgentName());
        entity.setUserId(event.getUserId());
        // userType 事件不携带（AgentEvent 仅有 userId），异步线程无 Sa-Token 上下文，按会话归属回查
        entity.setUserType(resolveUserType(event.getSessionId()));
        entity.setTenantId(parseLong(event.getTenantId()));
        // 链路追踪 ID：异步分发下取事件携带的值（发布线程采集），其次取当前线程上下文，无链路时回退 "N/A"
        entity.setTraceId(blankToDefault(firstNonBlank(event.getTraceId(), TraceContext.traceId()), "N/A"));

        try {
            auditLogMapper.insert(entity);
        } catch (Exception e) {
            log.warn("审计日志落库失败, eventType={}, error={}", event.getEventType(), e.getMessage());
        }
    }

    private String resolveUserType(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return "admin";
        }
        try {
            AiChatSessionEntity session = sessionMapper.selectOneByQuery(
                    QueryWrapper.create().where(AiChatSessionEntity::getSessionId).eq(sessionId));
            if (session != null && session.getUserType() != null && !session.getUserType().isBlank()) {
                return session.getUserType();
            }
        } catch (Exception e) {
            log.debug("[AgentAudit] 回查会话 userType 失败, sessionId={}, error={}", sessionId, e.getMessage());
        }
        return "admin";
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    /**
     * 工具结果的落库形态：默认按长度截断；对含外部原文的工具先摘正文再截断。
     *
     * <p>判不出来就不留：解析失败或结果不是对象（纯文本正文、JSON 字符串等）时无法证明其中没有原文，
     * 一律整体省略——审计宁可少信息，也不能把外部网页正文写进库。
     */
    private String auditResult(String toolName, String result) {
        if (result == null) {
            return "";
        }
        if (toolName == null || !BODY_OMITTED_TOOLS.contains(toolName.toLowerCase(Locale.ROOT))) {
            return truncate(result, RESULT_MAX_CHARS);
        }
        JsonNode node;
        try {
            node = objectMapper.readTree(result);
        } catch (RuntimeException e) {
            node = null;
        }
        if (node instanceof ObjectNode object) {
            JsonNode body = object.get("text");
            if (body == null || body.isNull()) {
                // 降级/错误回包没有正文，reason 等业务信息必须保住
                return truncate(result, RESULT_MAX_CHARS);
            }
            object.put("text", "<正文已省略 " + body.asString("").length() + " 字符>");
            try {
                return truncate(objectMapper.writeValueAsString(object), RESULT_MAX_CHARS);
            } catch (RuntimeException e) {
                return "<" + toolName + " 正文已省略（原文 " + result.length() + " 字符）>";
            }
        }
        return "<" + toolName + " 结果非结构化，正文已省略（原文 " + result.length() + " 字符）>";
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() > max ? text.substring(0, max) : text;
    }

    private int safeInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private Long parseLong(String value) {
        try {
            return value == null ? 0L : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
