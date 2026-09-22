package com.zhush.ai.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * AI 用量每日趋势视图对象（跨租户/智能体按日期再聚合后的一个点）。
 *
 * @author zhush
 */
@Data
public class AgentUsageTrendVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 统计日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate usageDate;

    /** 请求次数 */
    private Long requestCount;

    /** 输入 Token 累计 */
    private Long inputTokens;

    /** 输出 Token 累计 */
    private Long outputTokens;

    /** 总 Token 累计 */
    private Long totalTokens;

    /** 预估成本（元） */
    private BigDecimal cost;
}
