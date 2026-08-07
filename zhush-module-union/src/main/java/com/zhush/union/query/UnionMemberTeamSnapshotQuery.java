package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionMemberTeamSnapshotQuery
 * @Description 推广者团队快照表 分页查询请求
 * @Author zhush
 * @Date 2026/08/07
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "推广者团队快照表分页查询请求")
public class UnionMemberTeamSnapshotQuery extends BaseQuery {

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

}
