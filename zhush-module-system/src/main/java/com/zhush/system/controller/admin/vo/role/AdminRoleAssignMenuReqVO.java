package com.zhush.system.controller.admin.vo.role;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 角色分配菜单（权限）请求
 *
 * @author zhush
 */
@Data
public class AdminRoleAssignMenuReqVO {

    /** 角色ID */
    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    /** 菜单ID列表（为空表示清空权限） */
    private List<Long> menuIds;
}
