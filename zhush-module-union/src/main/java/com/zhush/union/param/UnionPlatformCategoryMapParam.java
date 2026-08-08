package com.zhush.union.param;

import com.zhush.union.entity.UnionPlatformCategoryMap;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionPlatformCategoryMapParam
 * @Description 平台类目映射表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionPlatformCategoryMap.class)
@Schema(description = "平台类目映射表新增/修改请求参数")
public class UnionPlatformCategoryMapParam {

    @Schema(description = "ID（新增时不需要传）")
    private Long id;

    @Schema(description = "平台")
    @NotNull(message = "平台不能为空")
    private Integer platform;

    @Schema(description = "平台类目ID")
    @NotBlank(message = "平台类目ID不能为空")
    private String platformCategoryId;

    @Schema(description = "平台类目名称")
    @NotBlank(message = "平台类目名称不能为空")
    private String platformCategoryName;

    @Schema(description = "平台父级类目ID")
    private String platformParentId;

    @Schema(description = "映射到union_category.id")
    private Long unionCategoryId;

    @Schema(description = "1-启用 0-禁用")
    private Integer status;

}
