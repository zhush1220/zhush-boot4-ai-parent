package com.zhush.union.param;

import com.zhush.union.entity.UnionCategory;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionCategoryParam
 * @Description 商品分类表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionCategory.class)
@Schema(description = "商品分类表新增/修改请求参数")
public class UnionCategoryParam {

    @Schema(description = "ID（新增时不需要传）")
    private Long id;

    @Schema(description = "父级ID（0表示顶级）")
    private Long parentId;

    @Schema(description = "分类名称")
    @NotBlank(message = "分类名称不能为空")
    private String name;

    @Schema(description = "分类图标URL")
    private String icon;

    @Schema(description = "sort_order")
    private Integer sortOrder;

    @Schema(description = "1-启用 0-禁用")
    private Integer status;

}
