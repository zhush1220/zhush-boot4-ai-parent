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

    private String userType;

    private Long userId;

    private Integer status;

    private Long tenantId;
}
