package com.zhush.system.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * @ClassName AdminUserAssignRoleParam
 * @Description 管理员分配角色请求
 * @Author zhush
 * @Date 2026/7/20 0020 22:30
 **/
@Data
@Schema(description = "管理员分配角色请求")
public class AdminUserAssignRoleParam {

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "角色ID列表（为空表示清空角色）")
    private List<Long> roleIds;

}
