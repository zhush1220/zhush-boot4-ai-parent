package com.zhush.system.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName AdminRoleQuery
 * @Description 角色分页查询请求
 * @Author zhush
 * @Date 2026/7/20 0020 22:30
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "角色分页查询请求")
public class AdminRoleQuery extends BaseQuery {

    @Schema(description = "角色名称（模糊匹配）")
    private String name;

    @Schema(description = "角色编码（模糊匹配）")
    private String code;

    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;

}
