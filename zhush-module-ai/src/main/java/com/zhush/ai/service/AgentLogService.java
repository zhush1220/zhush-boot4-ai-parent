package com.zhush.ai.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.ai.entity.AgentAuditLogEntity;
import com.zhush.ai.mapper.AgentAuditLogMapper;
import com.zhush.ai.query.AgentAuditLogQuery;
import com.zhush.ai.vo.AgentAuditLogVO;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 审计日志服务（只读分页）。
 *
 * @author zhush
 */
@Service
public class AgentLogService {

    private final AgentAuditLogMapper auditLogMapper;

    public AgentLogService(AgentAuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * 分页查询。
     */
    public PageResult<AgentAuditLogVO> page(AgentAuditLogQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(AgentAuditLogEntity::getAgentName, query.getAgentName())
                .eq(AgentAuditLogEntity::getSessionId, query.getSessionId())
                .eq(AgentAuditLogEntity::getEventType, query.getEventType())
                .eq(AgentAuditLogEntity::getToolName, query.getToolName());
        queryWrapper.orderBy("create_time", false);
        Page<AgentAuditLogEntity> page = auditLogMapper.selectPageX(query, queryWrapper);
        List<AgentAuditLogVO> list = new ArrayList<>(page.getRecords().size());
        for (AgentAuditLogEntity entity : page.getRecords()) {
            list.add(toVO(entity));
        }
        return PageResult.of(list, page.getTotalRow());
    }

    /**
     * 详情。
     */
    public AgentAuditLogVO get(Long id) {
        return toVO(auditLogMapper.selectOneById(id));
    }

    private AgentAuditLogVO toVO(AgentAuditLogEntity entity) {
        if (entity == null) {
            return null;
        }
        AgentAuditLogVO vo = new AgentAuditLogVO();
        vo.setId(entity.getId());
        vo.setTraceId(entity.getTraceId());
        vo.setSessionId(entity.getSessionId());
        vo.setAgentName(entity.getAgentName());
        vo.setEventType(entity.getEventType());
        vo.setToolName(entity.getToolName());
        vo.setToolArgs(entity.getToolArgs());
        vo.setToolResult(entity.getToolResult());
        vo.setDuration(entity.getDuration());
        vo.setTokenUsage(entity.getTokenUsage());
        vo.setStepCount(entity.getStepCount());
        vo.setUserId(entity.getUserId());
        vo.setUserType(entity.getUserType());
        vo.setTenantId(entity.getTenantId());
        vo.setResultCode(entity.getResultCode());
        vo.setResultMsg(entity.getResultMsg());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
