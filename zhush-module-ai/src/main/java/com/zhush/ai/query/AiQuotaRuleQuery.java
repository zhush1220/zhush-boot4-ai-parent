package com.zhush.ai.query;

import com.zhush.common.query.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * AI 配额规则分页查询。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiQuotaRuleQuery extends BaseQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户类型：admin / member */
    private String userType;

    /** 任务类型：chat/copywriting/docx/pdf/parse/image/video */
    private String taskType;

    /** 状态：0-停用 1-启用 */
    private Integer status;
}
