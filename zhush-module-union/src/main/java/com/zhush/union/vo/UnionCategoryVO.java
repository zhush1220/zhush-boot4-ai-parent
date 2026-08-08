package com.zhush.union.vo;

import com.zhush.union.entity.UnionCategory;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionCategoryVO
 * @Description 商品分类表 详情/列表返回
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionCategory.class)
@Schema(description = "商品分类表详情/列表返回")
public class UnionCategoryVO {

    @Schema(description = "ID")
    private Long id;

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

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
