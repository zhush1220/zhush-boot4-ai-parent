package com.zhush.ai.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * AI 用量汇总统计视图对象（当前筛选条件下的合计）。
 *
 * @author zhush
 */
@Data
public class AgentUsageSummaryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 请求次数合计 */
    private Long requestCount;

    /** 输入 Token 合计 */
    private Long inputTokens;

    /** 输出 Token 合计 */
    private Long outputTokens;

    /** 总 Token 合计 */
    private Long totalTokens;

    /** 预估成本合计（元） */
    private BigDecimal cost;

    /** 单次请求平均 Token（四舍五入） */
    private Long avgTokensPerRequest;

    /** 有用量的天数 */
    private Integer usageDays;
}
