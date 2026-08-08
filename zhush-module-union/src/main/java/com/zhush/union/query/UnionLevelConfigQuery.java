package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionLevelConfigQuery
 * @Description 层级佣金配置表 分页查询请求
 * @Author zhush
 * @Date 2026/08/08
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "层级佣金配置表分页查询请求")
public class UnionLevelConfigQuery extends BaseQuery {

    @Schema(description = "层级（1/2/3/...）")
    private Integer level;

    @Schema(description = "层级名称（如：合伙人/总监/经理）")
    private String levelName;

    @Schema(description = "晋升该层级所需最小团队人数")
    private Integer minTeamSize;

    @Schema(description = "排序")
    private Integer sortOrder;

}
