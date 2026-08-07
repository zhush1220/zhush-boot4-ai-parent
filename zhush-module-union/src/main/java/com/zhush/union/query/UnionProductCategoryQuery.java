package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionProductCategoryQuery
 * @Description 商品分类关联表 分页查询请求
 * @Author zhush
 * @Date 2026/08/07
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "商品分类关联表分页查询请求")
public class UnionProductCategoryQuery extends BaseQuery {

    @Schema(description = "product_id")
    private Long productId;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "排序")
    private Integer sortOrder;

}
