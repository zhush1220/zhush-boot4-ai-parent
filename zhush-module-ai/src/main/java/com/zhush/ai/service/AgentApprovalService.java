package com.zhush.ai.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.update.UpdateChain;
import com.zhush.ai.context.AiSessionContextResolver;
import com.zhush.ai.entity.AgentApprovalEntity;
import com.zhush.ai.mapper.AgentApprovalMapper;
import com.zhush.ai.query.AgentApprovalQuery;
import com.zhush.ai.vo.AgentApprovalVO;
import com.zhush.agentscope.safety.ToolApprovalHandler;
import com.zhush.common.response.PageResult;
import com.zhush.common.exception.ServiceExceptionUtil;
import com.zhush.mybatis.core.query.QueryWrapperX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.zhush.ai.enums.AiErrorCode.CONVERSATION_NOT_FOUND;

/**
 * 智能体审批服务：对接 starter 的 {@link ToolApprovalHandler}（Redis 状态）与 agent_approval 落库。
 *
 * <p>路线 B（原生确认）：命中 {@code reply_id} 非空的待确认记录时，审批后以 {@code resumed} 列
 * CAS 0→1 做幂等闸门，事务提交后触发同会话 key 的 {@code ConfirmResult} 续跑；
 * 路线 A（旧版工具层挂起）：保留 Redis 快路，续跑由后续 P3 代理执行。
 *
 * @author zhush
 */
@Service
public class AgentApprovalService {

    private static final Logger log = LoggerFactory.getLogger(AgentApprovalService.class);

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;

    private final ToolApprovalHandler approvalHandler;
    private final AgentApprovalMapper approvalMapper;
    private final AiSessionContextResolver contextResolver;
    private final ObjectMapper objectMapper;
    private final AgentChatService chatService;

    public AgentApprovalService(ToolApprovalHandler approvalHandler, AgentApprovalMapper approvalMapper,
                                AiSessionContextResolver contextResolver, ObjectMapper objectMapper,
                                AgentChatService chatService) {
        this.approvalHandler = approvalHandler;
        this.approvalMapper = approvalMapper;
        this.contextResolver = contextResolver;
        this.objectMapper = objectMapper;
        this.chatService = chatService;
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(String approvalId, java.util.Map<String, Object> modifiedArgs) {
        AgentApprovalEntity entity = approvalMapper.selectByApprovalId(approvalId);
        ServiceExceptionUtil.assertNotNull(entity, CONVERSATION_NOT_FOUND, approvalId);
        if (entity.getStatus() == null || entity.getStatus() != STATUS_PENDING) {
            // 幂等：非待审批状态（已通过/已驳回/已过期）直接返回，不重复执行与重复续跑
            log.info("审批已处理，跳过, approvalId={}, status={}", approvalId, entity.getStatus());
            return;
        }
        Long approver = contextResolver.resolve().getUserId();
        LocalDateTime now = LocalDateTime.now();
        if (isNative(entity)) {
            boolean won = casFinish(entity.getApprovalId(), approver, now, STATUS_APPROVED, modifiedArgs, null);
            if (!won) {
                log.info("原生审批已被处理（幂等跳过）, approvalId={}", approvalId);
                return;
            }
            final String agentName = entity.getAgentName();
            final String sessionId = entity.getSessionId();
            final String replyId = entity.getReplyId();
            runAfterCommit(() -> chatService.resumeNative(agentName, sessionId, replyId, true, modifiedArgs));
            return;
        }
        // 路线 A / 旧版：更新 Redis 快路 + DB（续跑由 P3 代理执行）
        approvalHandler.approve(approvalId, modifiedArgs);
        entity.setStatus(STATUS_APPROVED);
        entity.setApproveUserId(approver);
        entity.setApproveTime(now);
        if (modifiedArgs != null) {
            entity.setModifiedArgs(toJson(modifiedArgs));
        }
        approvalMapper.update(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(String approvalId, String reason) {
        AgentApprovalEntity entity = approvalMapper.selectByApprovalId(approvalId);
        ServiceExceptionUtil.assertNotNull(entity, CONVERSATION_NOT_FOUND, approvalId);
        if (entity.getStatus() == null || entity.getStatus() != STATUS_PENDING) {
            log.info("审批已处理，跳过, approvalId={}, status={}", approvalId, entity.getStatus());
            return;
        }
        Long approver = contextResolver.resolve().getUserId();
        LocalDateTime now = LocalDateTime.now();
        if (isNative(entity)) {
            boolean won = casFinish(entity.getApprovalId(), approver, now, STATUS_REJECTED, null, reason);
            if (!won) {
                log.info("原生审批已被处理（幂等跳过）, approvalId={}", approvalId);
                return;
            }
            final String agentName = entity.getAgentName();
            final String sessionId = entity.getSessionId();
            final String replyId = entity.getReplyId();
            // confirmed=false 嗂回驳回结果，配合 stopOnReject 让框架产出解释性收尾
            runAfterCommit(() -> chatService.resumeNative(agentName, sessionId, replyId, false, null));
            return;
        }
        approvalHandler.reject(approvalId, reason);
        entity.setStatus(STATUS_REJECTED);
        entity.setApproveUserId(approver);
        entity.setApproveTime(now);
        entity.setRejectReason(reason == null ? "" : reason);
        approvalMapper.update(entity);
    }

    /**
     * 路线 B 幂等闸门：以 {@code resumed} 列 0→1 的条件更新作为 CAS，返回 {@code true} 的调用才拥有续跑权。
     * 同时写入审批结果（状态/审批人/时间/改参/驳回原因）。
     *
     * <p>包级可见以便单测以 spy 打桩返回布尔结果，隔离框架 SQL 构建（测试分支/幂等而非 MyBatis-Flex 本身）。
     */
    boolean casFinish(String approvalId, Long approver, LocalDateTime now, int status,
                              java.util.Map<String, Object> modifiedArgs, String reason) {
        UpdateChain<AgentApprovalEntity> chain = UpdateChain.create(approvalMapper)
                .set(AgentApprovalEntity::getResumed, 1)
                .set(AgentApprovalEntity::getResumeTime, now)
                .set(AgentApprovalEntity::getStatus, status)
                .set(AgentApprovalEntity::getApproveUserId, approver)
                .set(AgentApprovalEntity::getApproveTime, now);
        if (modifiedArgs != null) {
            chain.set(AgentApprovalEntity::getModifiedArgs, toJson(modifiedArgs));
        }
        if (reason != null) {
            chain.set(AgentApprovalEntity::getRejectReason, reason);
        }
        return chain.where(AgentApprovalEntity::getApprovalId).eq(approvalId)
                .and(AgentApprovalEntity::getResumed).eq(0)
                .update();
    }

    private boolean isNative(AgentApprovalEntity entity) {
        return entity.getReplyId() != null && !entity.getReplyId().isBlank();
    }

    /**
     * 事务提交后再触发续跑（fire-and-forget），避免续跑的跨线程 DB 写读到未提交快照。
     */
    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
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
        vo.setReplyId(entity.getReplyId());
        vo.setResumed(entity.getResumed());
        vo.setRejectReason(entity.getRejectReason());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
