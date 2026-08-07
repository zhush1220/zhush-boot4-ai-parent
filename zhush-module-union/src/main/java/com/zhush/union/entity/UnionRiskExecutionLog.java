package com.zhush.union.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @ClassName UnionRiskExecutionLog
 * @Description 风控执行记录表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_risk_execution_log")
public class UnionRiskExecutionLog implements Serializable {

    @Serial
    private static final long serialVersionUID = -6000138469938077229L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 提现申请ID
     */
    private Long applyId;

    /**
     * 推广者ID
     */
    private Long memberId;

    /**
     * 风控上下文数据（所有检查字段的当前值）
     */
    private String contextData;

    /**
     * matched_rules
     */
    private String matchedRules;

    /**
     * 总风险分值
     */
    private Integer totalRiskScore;

    /**
     * 决策：1-自动通过 2-转人工审核 3-拒绝 4-降额处理
     */
    private Integer decision;

    /**
     * 决策原因
     */
    private String decisionReason;

    /**
     * 原始申请金额
     */
    private BigDecimal originalAmount;

    /**
     * 最终核定金额（降额后）
     */
    private BigDecimal finalAmount;

    /**
     * 执行耗时（毫秒）
     */
    private Integer executionDuration;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
