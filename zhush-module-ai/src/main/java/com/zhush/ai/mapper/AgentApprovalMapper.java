package com.zhush.ai.mapper;

import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.ai.entity.AgentApprovalEntity;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

/**
 * 智能体审批 Mapper
 *
 * @author zhush
 */
@Mapper
public interface AgentApprovalMapper extends BaseMapperX<AgentApprovalEntity> {

    /**
     * 按审批编号查询。
     */
    default AgentApprovalEntity selectByApprovalId(String approvalId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(AgentApprovalEntity::getApprovalId).eq(approvalId);
        return selectOneByQuery(queryWrapper);
    }
}
