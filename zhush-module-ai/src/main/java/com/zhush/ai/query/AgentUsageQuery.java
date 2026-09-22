package com.zhush.ai.query;

import com.zhush.common.query.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.time.LocalDate;

/**
 * AI 用量汇总分页查询。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentUsageQuery extends BaseQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 统计日期-起
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /**
     * 统计日期-止
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * 智能体名（模糊匹配）
     */
    private String agentName;

    /**
     * 租户ID
     */
    private Long tenantId;
}
