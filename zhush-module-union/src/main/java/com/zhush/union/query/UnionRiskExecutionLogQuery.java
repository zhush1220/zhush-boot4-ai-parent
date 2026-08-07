package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionRiskExecutionLogQuery
 * @Description 风控执行记录表 分页查询请求
 * @Author zhush
 * @Date 2026/08/07
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "风控执行记录表分页查询请求")
public class UnionRiskExecutionLogQuery extends BaseQuery {

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

    @Schema(description = "执行耗时（毫秒）")
    private Integer executionDuration;

}
