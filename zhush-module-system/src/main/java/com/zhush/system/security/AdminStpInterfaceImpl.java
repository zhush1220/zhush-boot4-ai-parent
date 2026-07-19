package com.zhush.system.security;

import cn.dev33.satoken.stp.StpInterface;
import com.zhush.satoken.core.LoginTypeConstants;
import com.zhush.system.service.AdminPermissionService;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 权限数据源
 *
 * <p>全局单例，按 {@code loginType} 区分账号体系：
 * <ul>
 *     <li>{@link LoginTypeConstants#ADMIN}：走 RBAC，返回真实角色/权限</li>
 *     <li>其他（如会员端）：无权限模型，返回空集合</li>
 * </ul>
 *
 * @author zhush
 */
@Component
public class AdminStpInterfaceImpl implements StpInterface {

    private final AdminPermissionService permissionService;

    public AdminStpInterfaceImpl(AdminPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        if (!LoginTypeConstants.ADMIN.equals(loginType)) {
            return Collections.emptyList();
        }
        return permissionService.getPermissionList(toUserId(loginId));
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (!LoginTypeConstants.ADMIN.equals(loginType)) {
            return Collections.emptyList();
        }
        return permissionService.getRoleCodeList(toUserId(loginId));
    }

    private Long toUserId(Object loginId) {
        return Long.valueOf(loginId.toString());
    }

}
