package com.zhush.system.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 角色列级字段授权请求（授权即分配字段对应的 sys_menu 按钮）
 *
 * @author zhush
 */
@Data
@Schema(description = "角色列级字段授权请求")
public class RoleFieldPermissionParam {

    @Schema(description = "角色ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    @Schema(description = "字段权限对应的菜单按钮ID列表（为空表示收回全部字段授权）")
    private List<Long> menuIds;

}
