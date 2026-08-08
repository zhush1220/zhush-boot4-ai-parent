package com.zhush.union.param;

import com.zhush.union.entity.UnionWithdrawRiskRecord;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionWithdrawRiskRecordParam
 * @Description 提现风控记录表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionWithdrawRiskRecord.class)
@Schema(description = "提现风控记录表新增/修改请求参数")
public class UnionWithdrawRiskRecordParam {

    @Schema(description = "ID（新增时不需要传）")
    private Long id;

    @Schema(description = "提现申请ID")
    @NotNull(message = "提现申请ID不能为空")
    private Long applyId;

    @Schema(description = "推广者ID")
    @NotNull(message = "推广者ID不能为空")
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
