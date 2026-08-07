package com.zhush.union.vo;

import com.zhush.union.entity.UnionMemberTeamSnapshot;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionMemberTeamSnapshotVO
 * @Description 推广者团队快照表 详情/列表返回
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@AutoMapper(target = UnionMemberTeamSnapshot.class)
@Schema(description = "推广者团队快照表详情/列表返回")
public class UnionMemberTeamSnapshotVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "推广者ID")
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
    private LocalDate snapshotDate;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
