package com.zhush.system.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * @ClassName AdminRoleAssignDataScopeParam
 * @Description 角色分配数据范围请求
 * @Author zhush
 * @Date 2026/7/13
 **/
@Data
@Schema(description = "角色分配数据范围请求")
public class AdminRoleAssignDataScopeParam {

    @Schema(description = "角色ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    @Schema(description = "数据权限：1-全部 2-本部门 3-本部门及以下 4-仅本人 5-自定义",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "数据权限不能为空")
    @Min(value = 1, message = "数据权限取值范围 1-5")
    @Max(value = 5, message = "数据权限取值范围 1-5")
    private Integer dataScope;

    @Schema(description = "自定义数据范围的部门ID列表（dataScope=5 时必传）")
    private List<Long> dataScopeDeptIds;

}
