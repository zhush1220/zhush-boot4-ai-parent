package com.zhush.union.vo;

import com.zhush.union.entity.UnionProductCategory;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionProductCategoryVO
 * @Description 商品分类关联表 详情/列表返回
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionProductCategory.class)
@Schema(description = "商品分类关联表详情/列表返回")
public class UnionProductCategoryVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "product_id")
    private Long productId;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
