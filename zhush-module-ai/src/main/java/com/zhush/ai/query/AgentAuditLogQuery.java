package com.zhush.ai.query;

import com.zhush.common.query.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * AI 审计日志分页查询。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentAuditLogQuery extends BaseQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    private String agentName;

    private String sessionId;

    private String eventType;

    private String toolName;
}
