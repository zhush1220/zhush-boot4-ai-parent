package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionMemberRelationQuery
 * @Description 推广者关系表 分页查询请求
 * @Author zhush
 * @Date 2026/08/08
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "推广者关系表分页查询请求")
public class UnionMemberRelationQuery extends BaseQuery {

    @Schema(description = "推广者ID（本人）")
    private Long memberId;

    @Schema(description = "上级推广者ID（直接邀请人）")
    private Long parentId;

    @Schema(description = "顶级推广者ID（根节点，用于快速归属）")
    private Long rootId;

    @Schema(description = "层级路径：例如 /1/23/45/ ，从根节点到本人的ID路径")
    private String path;

    @Schema(description = "当前层级：1表示顶级，2表示二级，以此类推")
    private Integer level;

    @Schema(description = "该节点以下的最大深度（子树深度，便于缓存）")
    private Integer maxDepth;

    @Schema(description = "状态：1-正常 2-已解除 3-已冻结")
    private Integer status;

    @Schema(description = "直属下级数量")
    private Integer totalChildren;

    @Schema(description = "整个团队人数（所有层级）")
    private Integer totalTeam;

    @Schema(description = "缓存版本号（变更时自增）")
    private Integer cacheVersion;

}
