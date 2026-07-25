package com.zhush.system.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @ClassName AdminMenuQuery
 * @Description 菜单列表查询请求（不分页，用于树形/列表过滤）
 * @Author zhush
 * @Date 2026/7/20 0020 22:19
 **/
@Data
@Schema(description = "菜单列表查询请求（不分页，用于树形/列表过滤）")
public class AdminMenuQuery {

    @Schema(description = "菜单名称（模糊）")
    private String name;

    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;

}
