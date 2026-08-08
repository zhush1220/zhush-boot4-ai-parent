package com.zhush.union.param;

import com.zhush.union.entity.UnionProductCategory;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionProductCategoryParam
 * @Description 商品分类关联表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionProductCategory.class)
@Schema(description = "商品分类关联表新增/修改请求参数")
public class UnionProductCategoryParam {

    @Schema(description = "ID（新增时不需要传）")
    private Long id;

    @Schema(description = "product_id")
    @NotNull(message = "product_id不能为空")
    private Long productId;

    @Schema(description = "分类ID")
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    @Schema(description = "排序")
    private Integer sortOrder;

}
