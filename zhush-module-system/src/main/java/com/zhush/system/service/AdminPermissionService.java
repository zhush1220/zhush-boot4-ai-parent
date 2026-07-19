package com.zhush.system.service;

import com.zhush.system.entity.SysAdminUserRole;
import com.zhush.system.entity.SysMenu;
import com.zhush.system.entity.SysRole;
import com.zhush.system.entity.SysRoleMenu;
import com.zhush.system.mapper.SysAdminUserRoleMapper;
import com.zhush.system.mapper.SysMenuMapper;
import com.zhush.system.mapper.SysRoleMapper;
import com.zhush.system.mapper.SysRoleMenuMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * 管理端权限服务
 *
 * <p>基于 RBAC：用户 → 角色 → 菜单（权限）。为 Sa-Token 的 {@code StpInterface} 提供数据源。
 *
 * @author zhush
 */
@Service
public class AdminPermissionService {

    /**
     * 超级管理员角色编码：拥有全部权限（Sa-Token 通配符 {@code *}）
     */
    public static final String SUPER_ADMIN_CODE = "super_admin";

    /**
     * 启用状态
     */
    private static final int STATUS_ENABLED = 1;

    private final SysAdminUserRoleMapper adminUserRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;

    public AdminPermissionService(SysAdminUserRoleMapper adminUserRoleMapper,
                                  SysRoleMapper roleMapper,
                                  SysRoleMenuMapper roleMenuMapper,
                                  SysMenuMapper menuMapper) {
        this.adminUserRoleMapper = adminUserRoleMapper;
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.menuMapper = menuMapper;
    }

    /**
     * 查询用户启用状态的角色编码列表
     */
    public List<String> getRoleCodeList(Long userId) {
        List<SysRole> roles = listEnabledRoles(userId);
        return roles.stream().map(SysRole::getCode).toList();
    }

    /**
     * 查询用户的权限标识列表；超级管理员返回 {@code ["*"]}
     */
    public List<String> getPermissionList(Long userId) {
        List<SysRole> roles = listEnabledRoles(userId);
        if (roles.isEmpty()) {
            return Collections.emptyList();
        }
        // 超级管理员拥有全部权限
        boolean isSuperAdmin = roles.stream().anyMatch(r -> SUPER_ADMIN_CODE.equals(r.getCode()));
        if (isSuperAdmin) {
            return List.of("*");
        }
        List<Long> roleIds = roles.stream().map(SysRole::getId).toList();
        List<SysRoleMenu> roleMenus = roleMenuMapper.selectListByRoleIds(roleIds);
        if (CollectionUtils.isEmpty(roleMenus)) {
            return Collections.emptyList();
        }
        List<Long> menuIds = roleMenus.stream().map(SysRoleMenu::getMenuId).distinct().toList();
        List<SysMenu> menus = menuMapper.selectListByIds(menuIds);
        return menus.stream()
                .filter(m -> m.getStatus() != null && m.getStatus() == STATUS_ENABLED)
                .map(SysMenu::getPermission)
                .filter(p -> p != null && !p.isBlank())
                .distinct()
                .toList();
    }

    /**
     * 查询用户启用状态的角色列表
     */
    private List<SysRole> listEnabledRoles(Long userId) {
        List<SysAdminUserRole> relations = adminUserRoleMapper.selectListByUserId(userId);
        if (CollectionUtils.isEmpty(relations)) {
            return Collections.emptyList();
        }
        List<Long> roleIds = relations.stream().map(SysAdminUserRole::getRoleId).distinct().toList();
        List<SysRole> roles = roleMapper.selectListByIds(roleIds);
        return roles.stream()
                .filter(r -> r.getStatus() != null && r.getStatus() == STATUS_ENABLED)
                .toList();
    }

}
