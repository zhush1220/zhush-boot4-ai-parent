package com.zhush.system.service;

import com.zhush.system.entity.SysAdminUserRole;
import com.zhush.system.entity.SysMenu;
import com.zhush.system.entity.SysRole;
import com.zhush.system.entity.SysRoleMenu;
import com.zhush.system.mapper.SysAdminUserRoleMapper;
import com.zhush.system.mapper.SysMenuMapper;
import com.zhush.system.mapper.SysRoleMapper;
import com.zhush.system.mapper.SysRoleMenuMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * RBAC 权限解析测试：超管通配符、禁用角色过滤、菜单权限去重与状态过滤
 */
@ExtendWith(MockitoExtension.class)
class AdminPermissionServiceTest {

    @Mock
    private SysAdminUserRoleMapper adminUserRoleMapper;
    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private SysRoleMenuMapper roleMenuMapper;
    @Mock
    private SysMenuMapper menuMapper;
    @InjectMocks
    private AdminPermissionService permissionService;

    private SysRole role(Long id, String code, int status) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setCode(code);
        role.setStatus(status);
        return role;
    }

    private SysAdminUserRole relation(Long userId, Long roleId) {
        SysAdminUserRole relation = new SysAdminUserRole();
        relation.setUserId(userId);
        relation.setRoleId(roleId);
        return relation;
    }

    private SysRoleMenu roleMenu(Long roleId, Long menuId) {
        SysRoleMenu roleMenu = new SysRoleMenu();
        roleMenu.setRoleId(roleId);
        roleMenu.setMenuId(menuId);
        return roleMenu;
    }

    private SysMenu menu(Long id, String permission, int status) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setPermission(permission);
        menu.setStatus(status);
        return menu;
    }

    @Test
    void noRolesReturnsEmptyLists() {
        when(adminUserRoleMapper.selectListByUserId(1L)).thenReturn(List.of());

        assertTrue(permissionService.getRoleCodeList(1L).isEmpty());
        assertTrue(permissionService.getPermissionList(1L).isEmpty());
    }

    @Test
    void disabledRolesAreFilteredOut() {
        when(adminUserRoleMapper.selectListByUserId(1L)).thenReturn(List.of(relation(1L, 10L)));
        when(roleMapper.selectListByIds(List.of(10L))).thenReturn(List.of(role(10L, "operator", 0)));

        assertTrue(permissionService.getRoleCodeList(1L).isEmpty());
        assertTrue(permissionService.getPermissionList(1L).isEmpty());
    }

    @Test
    void superAdminGetsWildcardPermission() {
        when(adminUserRoleMapper.selectListByUserId(1L)).thenReturn(List.of(relation(1L, 10L)));
        when(roleMapper.selectListByIds(List.of(10L))).thenReturn(List.of(role(10L, "super_admin", 1)));

        assertEquals(List.of("super_admin"), permissionService.getRoleCodeList(1L));
        assertEquals(List.of("*"), permissionService.getPermissionList(1L));
    }

    @Test
    void normalRoleGathersEnabledMenuPermissions() {
        when(adminUserRoleMapper.selectListByUserId(1L)).thenReturn(List.of(relation(1L, 10L), relation(1L, 11L)));
        when(roleMapper.selectListByIds(List.of(10L, 11L)))
                .thenReturn(List.of(role(10L, "operator", 1), role(11L, "audit", 1)));
        when(roleMenuMapper.selectListByRoleIds(List.of(10L, 11L)))
                .thenReturn(List.of(roleMenu(10L, 100L), roleMenu(10L, 101L), roleMenu(11L, 101L), roleMenu(11L, 102L)));
        when(menuMapper.selectListByIds(List.of(100L, 101L, 102L)))
                .thenReturn(List.of(
                        menu(100L, "union:order:view", 1),
                        menu(101L, "union:order:audit", 1),
                        menu(102L, "", 1)));

        // 菜单 102 权限标识为空被过滤；重复的 101 只保留一次
        assertEquals(List.of("union:order:view", "union:order:audit"),
                permissionService.getPermissionList(1L));
    }

    @Test
    void disabledMenusAreFilteredOut() {
        when(adminUserRoleMapper.selectListByUserId(1L)).thenReturn(List.of(relation(1L, 10L)));
        when(roleMapper.selectListByIds(List.of(10L))).thenReturn(List.of(role(10L, "operator", 1)));
        when(roleMenuMapper.selectListByRoleIds(List.of(10L))).thenReturn(List.of(roleMenu(10L, 100L)));
        when(menuMapper.selectListByIds(List.of(100L)))
                .thenReturn(List.of(menu(100L, "union:order:view", 0)));

        assertTrue(permissionService.getPermissionList(1L).isEmpty());
    }

    @Test
    void duplicatePermissionsAreDeduplicated() {
        when(adminUserRoleMapper.selectListByUserId(1L)).thenReturn(List.of(relation(1L, 10L)));
        when(roleMapper.selectListByIds(List.of(10L))).thenReturn(List.of(role(10L, "operator", 1)));
        when(roleMenuMapper.selectListByRoleIds(List.of(10L)))
                .thenReturn(List.of(roleMenu(10L, 100L), roleMenu(10L, 200L)));
        when(menuMapper.selectListByIds(List.of(100L, 200L)))
                .thenReturn(List.of(menu(100L, "union:order:view", 1), menu(200L, "union:order:view", 1)));

        assertEquals(List.of("union:order:view"), permissionService.getPermissionList(1L));
    }

    @Test
    void listEnabledRolesFiltersDisabled() {
        when(adminUserRoleMapper.selectListByUserId(1L))
                .thenReturn(List.of(relation(1L, 10L), relation(1L, 11L)));
        when(roleMapper.selectListByIds(List.of(10L, 11L)))
                .thenReturn(List.of(role(10L, "operator", 1), role(11L, "audit", 0)));

        List<SysRole> roles = permissionService.listEnabledRoles(1L);

        assertEquals(1, roles.size());
        assertEquals("operator", roles.get(0).getCode());
    }
}
