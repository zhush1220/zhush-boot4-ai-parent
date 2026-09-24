package com.zhush.ai.query;

import com.zhush.common.query.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * AI 会话分页查询。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiSessionQuery extends BaseQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    private String agentName;

    /** 用户类型：无 query-all 时被服务端覆盖为当前登录人 */
    private String userType;

    /**
     * 目标用户：仅持有 {@code ai:session:query-all} 时生效；
     * 否则服务端强制覆盖为当前登录人（防越权列举，前端无需传）。
     */
    private Long userId;

    private Integer status;

    private Long tenantId;
}
