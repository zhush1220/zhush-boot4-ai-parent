package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionWithdrawRiskRecordQuery
 * @Description 提现风控记录表 分页查询请求
 * @Author zhush
 * @Date 2026/08/08
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "提现风控记录表分页查询请求")
public class UnionWithdrawRiskRecordQuery extends BaseQuery {

    @Schema(description = "提现申请ID")
    private Long applyId;

    @Schema(description = "推广者ID")
    private Long memberId;

    @Schema(description = "风险评分（0-100，越高越危险）")
    private Integer riskScore;

    @Schema(description = "风险等级：1-低 2-中 3-高 4-极高")
    private Integer riskLevel;

    @Schema(description = "check_items")
    private String checkItems;

    @Schema(description = "hit_rules")
    private String hitRules;

    @Schema(description = "建议：PASS-通过 MANUAL-人工审核 REJECT-拒绝")
    private String suggestion;

}
