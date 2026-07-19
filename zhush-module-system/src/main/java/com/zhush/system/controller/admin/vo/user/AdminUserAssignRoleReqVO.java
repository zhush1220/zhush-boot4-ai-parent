package com.zhush.system.controller.admin.vo.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 管理员分配角色请求
 *
 * @author zhush
 */
@Data
public class AdminUserAssignRoleReqVO {

    /** 用户ID */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /** 角色ID列表（为空表示清空角色） */
    private List<Long> roleIds;
}
