package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionCategoryQuery
 * @Description 商品分类表 分页查询请求
 * @Author zhush
 * @Date 2026/08/08
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "商品分类表分页查询请求")
public class UnionCategoryQuery extends BaseQuery {

    @Schema(description = "父级ID（0表示顶级）")
    private Long parentId;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "分类图标URL")
    private String icon;

    @Schema(description = "sort_order")
    private Integer sortOrder;

    @Schema(description = "1-启用 0-禁用")
    private Integer status;

}
