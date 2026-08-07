package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionMemberLevelQuery
 * @Description 推广者等级配置表 分页查询请求
 * @Author zhush
 * @Date 2026/08/07
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "推广者等级配置表分页查询请求")
public class UnionMemberLevelQuery extends BaseQuery {

    @Schema(description = "等级编码：L1/L2/L3")
    private String levelCode;

    @Schema(description = "等级名称：见习/正式/黄金/钻石/合伙人")
    private String levelName;

    @Schema(description = "晋升所需最小团队人数")
    private Integer minTeamSize;

    @Schema(description = "status")
    private Integer status;

    @Schema(description = "sort_order")
    private Integer sortOrder;

}
