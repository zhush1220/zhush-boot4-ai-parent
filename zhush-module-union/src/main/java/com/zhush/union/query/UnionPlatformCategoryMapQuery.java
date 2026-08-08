package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionPlatformCategoryMapQuery
 * @Description 平台类目映射表 分页查询请求
 * @Author zhush
 * @Date 2026/08/08
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "平台类目映射表分页查询请求")
public class UnionPlatformCategoryMapQuery extends BaseQuery {

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

}
