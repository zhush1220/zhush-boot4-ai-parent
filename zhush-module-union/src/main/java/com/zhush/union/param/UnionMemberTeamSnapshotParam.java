package com.zhush.union.param;

import com.zhush.union.entity.UnionMemberTeamSnapshot;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionMemberTeamSnapshotParam
 * @Description 推广者团队快照表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionMemberTeamSnapshot.class)
@Schema(description = "推广者团队快照表新增/修改请求参数")
public class UnionMemberTeamSnapshotParam {

    @Schema(description = "ID（新增时不需要传）")
    private Long id;

    @Schema(description = "推广者ID")
    @NotNull(message = "推广者ID不能为空")
    private Long memberId;

    @Schema(description = "一级下级数")
    private Integer level1Count;

    @Schema(description = "二级下级数")
    private Integer level2Count;

    @Schema(description = "三级下级数")
    private Integer level3Count;

    @Schema(description = "团队总人数")
    private Integer totalTeamCount;

    @Schema(description = "团队累计贡献佣金")
    private BigDecimal teamTotalCommission;

    @Schema(description = "本月团队贡献佣金")
    private BigDecimal teamMonthCommission;

    @Schema(description = "快照日期")
    @NotNull(message = "快照日期不能为空")
    private LocalDate snapshotDate;

}
