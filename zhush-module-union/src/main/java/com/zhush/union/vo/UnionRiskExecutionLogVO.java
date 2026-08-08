package com.zhush.union.vo;

import com.zhush.union.entity.UnionRiskExecutionLog;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionRiskExecutionLogVO
 * @Description 风控执行记录表 详情/列表返回
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionRiskExecutionLog.class)
@Schema(description = "风控执行记录表详情/列表返回")
public class UnionRiskExecutionLogVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "提现申请ID")
    private Long applyId;

    @Schema(description = "推广者ID")
    private Long memberId;

    @Schema(description = "风控上下文数据（所有检查字段的当前值）")
    private String contextData;

    @Schema(description = "matched_rules")
    private String matchedRules;

    @Schema(description = "总风险分值")
    private Integer totalRiskScore;

    @Schema(description = "决策：1-自动通过 2-转人工审核 3-拒绝 4-降额处理")
    private Integer decision;

    @Schema(description = "决策原因")
    private String decisionReason;

    @Schema(description = "原始申请金额")
    private BigDecimal originalAmount;

    @Schema(description = "最终核定金额（降额后）")
    private BigDecimal finalAmount;

    @Schema(description = "执行耗时（毫秒）")
    private Integer executionDuration;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
