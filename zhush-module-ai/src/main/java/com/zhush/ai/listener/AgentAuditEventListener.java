package com.zhush.ai.listener;

import com.zhush.ai.entity.AgentAuditLogEntity;
import com.zhush.ai.mapper.AgentAuditLogMapper;
import com.zhush.agentscope.event.AgentApprovalRequiredEvent;
import com.zhush.agentscope.event.AgentErrorEvent;
import com.zhush.agentscope.event.AgentEvent;
import com.zhush.agentscope.event.AgentEventListener;
import com.zhush.agentscope.event.AgentEventType;
import com.zhush.agentscope.event.AgentFinishedEvent;
import com.zhush.agentscope.event.AgentStartedEvent;
import com.zhush.agentscope.event.AgentToolCalledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 审计事件监听器：将会话生命周期事件落库到 {@code agent_audit_log}。
 *
 * <p>跳过高频的文本增量事件（{@link com.zhush.agentscope.event.AgentTextDeltaEvent}），保留关键事件留痕。
 *
 * @author zhush
 */
@Component
public class AgentAuditEventListener implements AgentEventListener {

    private static final Logger log = LoggerFactory.getLogger(AgentAuditEventListener.class);

    private final AgentAuditLogMapper auditLogMapper;

    public AgentAuditEventListener(AgentAuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
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
            entity.setTokenUsage(finishedEvent.getOutputTokens() + finishedEvent.getInputTokens());
            entity.setStepCount(finishedEvent.getStepCount());
        } else if (event instanceof AgentToolCalledEvent toolEvent) {
            entity.setEventType("TOOL_CALLED");
            entity.setToolName(toolEvent.getToolName());
            entity.setToolArgs(toolEvent.getArguments());
            entity.setToolResult(toolEvent.getResult());
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
        entity.setUserType("admin");
        entity.setTenantId(parseLong(event.getTenantId()));

        try {
            auditLogMapper.insert(entity);
        } catch (Exception e) {
            log.warn("审计日志落库失败, eventType={}, error={}", event.getEventType(), e.getMessage());
        }
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
