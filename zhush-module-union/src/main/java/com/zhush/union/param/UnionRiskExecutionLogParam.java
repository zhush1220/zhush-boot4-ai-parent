package com.zhush.union.param;

import com.zhush.union.entity.UnionRiskExecutionLog;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionRiskExecutionLogParam
 * @Description 风控执行记录表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionRiskExecutionLog.class)
@Schema(description = "风控执行记录表新增/修改请求参数")
public class UnionRiskExecutionLogParam {

    @Schema(description = "ID（新增时不需要传）")
    private Long id;

    @Schema(description = "提现申请ID")
    @NotNull(message = "提现申请ID不能为空")
    private Long applyId;

    @Schema(description = "推广者ID")
    @NotNull(message = "推广者ID不能为空")
    private Long memberId;

    @Schema(description = "风控上下文数据（所有检查字段的当前值）")
    @NotBlank(message = "风控上下文数据（所有检查字段的当前值）不能为空")
    private String contextData;

    @Schema(description = "matched_rules")
    private String matchedRules;

    @Schema(description = "总风险分值")
    private Integer totalRiskScore;

    @Schema(description = "决策：1-自动通过 2-转人工审核 3-拒绝 4-降额处理")
    @NotNull(message = "决策：1-自动通过 2-转人工审核 3-拒绝 4-降额处理不能为空")
    private Integer decision;

    @Schema(description = "决策原因")
    private String decisionReason;

    @Schema(description = "原始申请金额")
    private BigDecimal originalAmount;

    @Schema(description = "最终核定金额（降额后）")
    private BigDecimal finalAmount;

    @Schema(description = "执行耗时（毫秒）")
    private Integer executionDuration;

}
