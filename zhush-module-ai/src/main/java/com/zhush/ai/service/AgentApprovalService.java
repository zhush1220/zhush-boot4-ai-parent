package com.zhush.ai.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.mybatisflex.core.paginate.Page;
import com.zhush.ai.context.AiSessionContextResolver;
import com.zhush.ai.entity.AgentApprovalEntity;
import com.zhush.ai.mapper.AgentApprovalMapper;
import com.zhush.ai.query.AgentApprovalQuery;
import com.zhush.ai.vo.AgentApprovalVO;
import com.zhush.agentscope.safety.ToolApprovalHandler;
import com.zhush.common.response.PageResult;
import com.zhush.common.exception.ServiceExceptionUtil;
import com.zhush.mybatis.core.query.QueryWrapperX;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.zhush.ai.enums.AiErrorCode.CONVERSATION_NOT_FOUND;

/**
 * 智能体审批服务：对接 starter 的 {@link ToolApprovalHandler}（Redis 状态）与 agent_approval 落库。
 *
 * @author zhush
 */
@Service
public class AgentApprovalService {

    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;

    private final ToolApprovalHandler approvalHandler;
    private final AgentApprovalMapper approvalMapper;
    private final AiSessionContextResolver contextResolver;
    private final ObjectMapper objectMapper;

    public AgentApprovalService(ToolApprovalHandler approvalHandler, AgentApprovalMapper approvalMapper,
                                AiSessionContextResolver contextResolver, ObjectMapper objectMapper) {
        this.approvalHandler = approvalHandler;
        this.approvalMapper = approvalMapper;
        this.contextResolver = contextResolver;
        this.objectMapper = objectMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(String approvalId, java.util.Map<String, Object> modifiedArgs) {
        AgentApprovalEntity entity = approvalMapper.selectByApprovalId(approvalId);
        ServiceExceptionUtil.assertNotNull(entity, CONVERSATION_NOT_FOUND, approvalId);
        approvalHandler.approve(approvalId, modifiedArgs);
        entity.setStatus(STATUS_APPROVED);
        entity.setApproveUserId(contextResolver.resolve().getUserId());
        entity.setApproveTime(LocalDateTime.now());
        if (modifiedArgs != null) {
            entity.setModifiedArgs(toJson(modifiedArgs));
        }
        approvalMapper.update(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(String approvalId, String reason) {
        AgentApprovalEntity entity = approvalMapper.selectByApprovalId(approvalId);
        ServiceExceptionUtil.assertNotNull(entity, CONVERSATION_NOT_FOUND, approvalId);
        approvalHandler.reject(approvalId, reason);
        entity.setStatus(STATUS_REJECTED);
        entity.setApproveUserId(contextResolver.resolve().getUserId());
        entity.setApproveTime(LocalDateTime.now());
        entity.setRejectReason(reason == null ? "" : reason);
        approvalMapper.update(entity);
    }

    public PageResult<AgentApprovalVO> page(AgentApprovalQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(AgentApprovalEntity::getStatus, query.getStatus())
                .eq(AgentApprovalEntity::getToolName, query.getToolName())
                .eq(AgentApprovalEntity::getAgentName, query.getAgentName());
        queryWrapper.orderBy("create_time", false);
        Page<AgentApprovalEntity> page = approvalMapper.selectPageX(query, queryWrapper);
        List<AgentApprovalVO> list = new ArrayList<>(page.getRecords().size());
        for (AgentApprovalEntity entity : page.getRecords()) {
            list.add(toVO(entity));
        }
        return PageResult.of(list, page.getTotalRow());
    }

    private String toJson(java.util.Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalStateException("序列化审批改参失败", e);
        }
    }

    private AgentApprovalVO toVO(AgentApprovalEntity entity) {
        AgentApprovalVO vo = new AgentApprovalVO();
        vo.setId(entity.getId());
        vo.setApprovalId(entity.getApprovalId());
        vo.setSessionId(entity.getSessionId());
        vo.setAgentName(entity.getAgentName());
        vo.setToolName(entity.getToolName());
        vo.setToolArgs(entity.getToolArgs());
        vo.setModifiedArgs(entity.getModifiedArgs());
        vo.setProposerId(entity.getProposerId());
        vo.setProposerType(entity.getProposerType());
        vo.setProposerName(entity.getProposerName());
        vo.setStatus(entity.getStatus());
        vo.setRejectReason(entity.getRejectReason());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
