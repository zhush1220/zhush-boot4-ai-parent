package com.zhush.ai.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.zhush.agentscope.safety.ApprovalPersister;
import com.zhush.agentscope.safety.ApprovalRequest;
import com.zhush.ai.entity.AgentApprovalEntity;
import com.zhush.ai.mapper.AgentApprovalMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 审批落库实现（P0）：把 starter {@code approvalGate} 生成的 {@link ApprovalRequest} 写入
 * {@code agent_approval}，修复「审批只进 Redis、管理端列表恒空、approve 必抛会话不存在」的致命缺陷。
 *
 * <p>由 starter 侧 {@code DefaultAgentTemplate} 经 {@link ApprovalPersister} SPI 回调，
 * starter 不直连业务库；异常由调用方吞掉，此处只保证映射与插入正确。
 *
 * @author zhush
 */
@Component
public class AgentApprovalPersister implements ApprovalPersister {

    private static final Logger log = LoggerFactory.getLogger(AgentApprovalPersister.class);

    /** 状态：0-待审批 */
    private static final int STATUS_PENDING = 0;

    private final AgentApprovalMapper approvalMapper;
    private final ObjectMapper objectMapper;

    public AgentApprovalPersister(AgentApprovalMapper approvalMapper, ObjectMapper objectMapper) {
        this.approvalMapper = approvalMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void persist(ApprovalRequest request) {
        if (request == null || request.getApprovalId() == null) {
            return;
        }
        AgentApprovalEntity entity = new AgentApprovalEntity();
        entity.setApprovalId(request.getApprovalId());
        entity.setSessionId(request.getSessionId() == null ? "" : request.getSessionId());
        entity.setAgentName(request.getAgentName() == null ? "" : request.getAgentName());
        entity.setToolName(request.getToolName() == null ? "" : request.getToolName());
        entity.setToolArgs(toJson(request));
        entity.setProposerId(request.getProposerId() == null ? 0L : request.getProposerId());
        entity.setProposerType(request.getProposerType() == null ? "admin" : request.getProposerType());
        entity.setProposerName(request.getProposerName() == null ? "" : request.getProposerName());
        entity.setStatus(STATUS_PENDING);
        entity.setReplyId(request.getReplyId() == null ? "" : request.getReplyId());
        entity.setResumed(0);
        if (request.getExpireTime() != null) {
            entity.setExpireTime(LocalDateTime.ofInstant(request.getExpireTime(), ZoneId.systemDefault()));
        }
        approvalMapper.insert(entity);
        log.info("审批记录落库成功, approvalId={}, tool={}, sessionId={}",
                entity.getApprovalId(), entity.getToolName(), entity.getSessionId());
    }

    private String toJson(ApprovalRequest request) {
        try {
            return objectMapper.writeValueAsString(request.getArguments() == null ? java.util.Map.of() : request.getArguments());
        } catch (JacksonException e) {
            log.warn("审批入参序列化失败, approvalId={}", request.getApprovalId(), e);
            return "{}";
        }
    }
}
