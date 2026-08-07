package com.zhush.union.vo;

import com.zhush.union.entity.UnionWithdrawRiskRecord;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionWithdrawRiskRecordVO
 * @Description 提现风控记录表 详情/列表返回
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@AutoMapper(target = UnionWithdrawRiskRecord.class)
@Schema(description = "提现风控记录表详情/列表返回")
public class UnionWithdrawRiskRecordVO {

    @Schema(description = "ID")
    private Long id;

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

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
