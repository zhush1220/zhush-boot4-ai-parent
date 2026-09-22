package com.zhush.ai.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AI 用量汇总视图对象（按租户/智能体/日聚合的一行）。
 *
 * @author zhush
 */
@Data
public class AgentUsageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    /** 统计日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate usageDate;

    /** 租户ID */
    private Long tenantId;

    /** 智能体名（空表示未区分智能体的汇总） */
    private String agentName;

    /** 请求次数 */
    private Integer requestCount;

    /** 输入 Token 累计 */
    private Long inputTokens;

    /** 输出 Token 累计 */
    private Long outputTokens;

    /** 总 Token 累计 */
    private Long totalTokens;

    /** 预估成本（元，按模型单价折算；未定价记 0） */
    private BigDecimal cost;

    /** 最后累计时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
