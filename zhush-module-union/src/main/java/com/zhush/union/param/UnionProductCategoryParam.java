package com.zhush.union.param;

import com.zhush.union.entity.UnionProductCategory;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionProductCategoryParam
 * @Description 商品分类关联表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@AutoMapper(target = UnionProductCategory.class)
@Schema(description = "商品分类关联表新增/修改请求参数")
public class UnionProductCategoryParam {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "product_id")
    private Long productId;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "排序")
    private Integer sortOrder;

}
