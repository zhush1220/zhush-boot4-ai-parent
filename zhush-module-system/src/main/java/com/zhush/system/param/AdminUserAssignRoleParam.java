package com.zhush.system.param;

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
public class AdminUserAssignRoleParam {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 角色ID列表(为空表示清空角色)
     */
    private List<Long> roleIds;

}
