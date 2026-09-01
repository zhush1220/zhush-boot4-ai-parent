package com.zhush.ai.query;

import com.zhush.common.query.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 审批任务分页查询。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentApprovalQuery extends BaseQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer status;

    private String toolName;

    private String agentName;
}
