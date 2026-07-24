package com.zhush.system.param;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * @ClassName AdminRoleAssignMenuParam
 * @Description 角色分配菜单（权限）请求
 * @Author zhush
 * @Date 2026/7/20 0020 22:30
 **/
@Data
public class AdminRoleAssignMenuParam {

    /**
     * 角色ID
     */
    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    /**
     * 菜单ID列表（为空表示清空权限）
     */
    private List<Long> menuIds;

}
