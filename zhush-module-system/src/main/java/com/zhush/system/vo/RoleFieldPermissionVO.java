package com.zhush.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 角色列级字段授权视图（全部受控字段 + 角色已授权菜单ID）
 *
 * @author zhush
 */
@Data
@Schema(description = "角色列级字段授权视图")
public class RoleFieldPermissionVO {

    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "全部受控字段")
    private List<DataFieldVO> fields;

    @Schema(description = "角色已授权的字段菜单ID列表")
    private List<Long> grantedMenuIds;

}
