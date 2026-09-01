package com.zhush.ai.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * AI 用量汇总实体（agent_token_usage）
 *
 * <p>按租户/智能体/日聚合，用于成本上限与监控。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("agent_token_usage")
public class AgentTokenUsageEntity extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 统计日期 */
    private LocalDate usageDate;

    /** 租户ID */
    private Long tenantId;

    /** 智能体名 */
    private String agentName;

    /** 请求次数 */
    private Integer requestCount;

    /** 输入 Token 累计 */
    private Long inputTokens;

    /** 输出 Token 累计 */
    private Long outputTokens;

    /** 总 Token 累计 */
    private Long totalTokens;

    /** 预估成本（按模型单价折算） */
    private BigDecimal cost;
}
