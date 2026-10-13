package com.zhush.system.security;

import com.zhush.data.permission.core.PermissionChecker;
import org.springframework.stereotype.Component;

/**
 * 管理端字段权限校验器（框架 {@link PermissionChecker} SPI 的实现）
 *
 * <p>委托 {@link FieldPermissionResolver} 从数据库读取当前登录管理员的角色字段权限点，
 * 判定口径与 RBAC 权限点（{@code sys_menu}/{@code sys_role_menu}）保持一致，
 * 未登录或无对应权限点时返回 {@code false}（字段将被置空/脱敏）。
 *
 * @author zhush
 */
@Component
public class AdminPermissionChecker implements PermissionChecker {

    private final FieldPermissionResolver fieldPermissionResolver;

    public AdminPermissionChecker(FieldPermissionResolver fieldPermissionResolver) {
        this.fieldPermissionResolver = fieldPermissionResolver;
    }

    @Override
    public boolean hasPermission(String permission) {
        return fieldPermissionResolver.hasPermission(permission);
    }

}
