package com.zhush.system.security;

import com.zhush.satoken.core.StpAdminUtil;
import com.zhush.system.service.AdminPermissionService;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * 字段权限解析器（列级授权的数据来源）
 *
 * <p>从数据库读取当前登录管理员的角色字段权限点集合（RBAC：用户 → 角色 → {@code sys_menu} 权限点），
 * 供 {@link AdminPermissionChecker} 注入到框架 {@code PermissionChecker} 上下文，
 * 决定响应体中被 {@code @FieldPermission} 标注的字段是原样返回还是置空/脱敏。
 *
 * <p>权限点单一事实来源仍是 {@code sys_menu}/{@code sys_role_menu}，本组件不引入第二套存储；
 * 超级管理员角色对应通配 {@code "*"}，直接放行全部字段。
 *
 * @author zhush
 */
@Component
public class FieldPermissionResolver {

    /**
     * 超级管理员通配权限标识（与 {@link AdminPermissionService#getPermissionList} 约定一致）
     */
    private static final String WILDCARD = "*";

    private final AdminPermissionService permissionService;

    public FieldPermissionResolver(AdminPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * 解析当前登录管理员的权限点集合（未登录返回空集）
     */
    public Set<String> resolveCurrentPermissions() {
        Long userId = StpAdminUtil.getLoginIdDefaultNull();
        return userId == null ? Set.of() : resolvePermissions(userId);
    }

    /**
     * 解析指定管理员的权限点集合（DB 读取）
     */
    public Set<String> resolvePermissions(Long userId) {
        if (userId == null) {
            return Set.of();
        }
        return new HashSet<>(permissionService.getPermissionList(userId));
    }

    /**
     * 当前登录管理员是否拥有指定权限点（通配 {@code "*"} 视为拥有全部）
     */
    public boolean hasPermission(String permission) {
        Set<String> permissions = resolveCurrentPermissions();
        if (permissions.isEmpty()) {
            return false;
        }
        return permissions.contains(WILDCARD) || permissions.contains(permission);
    }

}
