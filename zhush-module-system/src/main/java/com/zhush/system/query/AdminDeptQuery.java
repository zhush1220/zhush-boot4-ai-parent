package com.zhush.system.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @ClassName AdminDeptQuery
 * @Description 部门列表查询请求（不分页，用于树形/列表过滤）
 * @Author zhush
 * @Date 2026/7/13
 **/
@Data
@Schema(description = "部门列表查询请求（不分页，用于树形/列表过滤）")
public class AdminDeptQuery {

    @Schema(description = "部门名称（模糊）")
    private String name;

    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;

}
