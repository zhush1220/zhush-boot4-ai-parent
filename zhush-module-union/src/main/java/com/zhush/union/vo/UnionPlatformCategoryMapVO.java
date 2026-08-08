package com.zhush.union.vo;

import com.zhush.union.entity.UnionPlatformCategoryMap;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionPlatformCategoryMapVO
 * @Description 平台类目映射表 详情/列表返回
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionPlatformCategoryMap.class)
@Schema(description = "平台类目映射表详情/列表返回")
public class UnionPlatformCategoryMapVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "平台")
    private Integer platform;

    @Schema(description = "平台类目ID")
    private String platformCategoryId;

    @Schema(description = "平台类目名称")
    private String platformCategoryName;

    @Schema(description = "平台父级类目ID")
    private String platformParentId;

    @Schema(description = "映射到union_category.id")
    private Long unionCategoryId;

    @Schema(description = "1-启用 0-禁用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
