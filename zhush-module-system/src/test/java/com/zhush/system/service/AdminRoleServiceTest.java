package com.zhush.system.service;

import com.zhush.common.exception.ServiceException;
import com.zhush.system.entity.SysAdminUserRole;
import com.zhush.system.entity.SysMenu;
import com.zhush.system.entity.SysRole;
import com.zhush.system.entity.SysRoleMenu;
import com.zhush.system.mapper.SysAdminUserRoleMapper;
import com.zhush.system.mapper.SysMenuMapper;
import com.zhush.system.mapper.SysRoleMapper;
import com.zhush.system.mapper.SysRoleMenuMapper;
import com.zhush.system.param.AdminRoleAssignDataScopeParam;
import com.zhush.system.param.AdminRoleParam;
import com.zhush.system.vo.AdminRoleVO;
import io.github.linpeilie.Converter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 角色管理服务测试：编码唯一性、菜单授权过滤、删除保护、数据范围分配
 */
@ExtendWith(MockitoExtension.class)
class AdminRoleServiceTest {

    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private SysRoleMenuMapper roleMenuMapper;
    @Mock
    private SysMenuMapper menuMapper;
    @Mock
    private SysAdminUserRoleMapper adminUserRoleMapper;
    @Mock
    private Converter converter;
    @InjectMocks
    private AdminRoleService adminRoleService;

    private AdminRoleParam roleParam(Long id, String code) {
        AdminRoleParam param = new AdminRoleParam();
        param.setId(id);
        param.setCode(code);
        return param;
    }

    private SysRole role(Long id, String code) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setCode(code);
        role.setStatus(1);
        return role;
    }

    @Test
    void createRoleRejectsDuplicateCode() {
        when(roleMapper.selectByCode("operator")).thenReturn(role(99L, "operator"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> adminRoleService.createRole(roleParam(null, "operator")));

        assertEquals(1001103, ex.getCode());
    }

    @Test
    void createRoleDefaultsEnabledAndFiltersMenus() {
        SysRole converted = new SysRole();
        converted.setId(100L);
        doReturn(converted).when(converter).<Object, SysRole>convert(ArgumentMatchers.<Object>any(), eq(SysRole.class));
        when(menuMapper.selectListByIds(List.of(1L, 2L))).thenReturn(List.of(menuOf(1L)));

        AdminRoleParam param = roleParam(null, "operator");
        param.setMenuIds(List.of(1L, 2L));

        Long id = adminRoleService.createRole(param);

        assertEquals(100L, id);
        ArgumentCaptor<SysRole> roleCaptor = ArgumentCaptor.forClass(SysRole.class);
        verify(roleMapper).insert(roleCaptor.capture());
        assertEquals(1, roleCaptor.getValue().getStatus());
        // 菜单过滤：不存在的菜单 2 被剔除
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SysRoleMenu>> relCaptor = ArgumentCaptor.forClass(List.class);
        verify(roleMenuMapper).insertBatchWithDb(relCaptor.capture());
        assertEquals(1, relCaptor.getValue().size());
        assertEquals(1L, relCaptor.getValue().get(0).getMenuId());
        assertEquals(100L, relCaptor.getValue().get(0).getRoleId());
    }

    @Test
    void updateRoleNotFoundThrows() {
        when(roleMapper.selectOneById(1L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> adminRoleService.updateRole(roleParam(1L, "operator")));

        assertEquals(1001101, ex.getCode());
    }

    @Test
    void updateRoleKeepsCodeImmutable() {
        when(roleMapper.selectOneById(1L)).thenReturn(role(1L, "operator"));
        SysRole converted = new SysRole();
        converted.setId(1L);
        converted.setCode("renamed");
        doReturn(converted).when(converter).<Object, SysRole>convert(ArgumentMatchers.<Object>any(), eq(SysRole.class));

        AdminRoleParam param = roleParam(1L, "renamed");
        param.setMenuIds(null);

        adminRoleService.updateRole(param);

        ArgumentCaptor<SysRole> captor = ArgumentCaptor.forClass(SysRole.class);
        verify(roleMapper).update(captor.capture());
        // 编码不可修改
        assertNull(captor.getValue().getCode());
        // menuIds 为 null 时不触碰菜单授权
        verify(roleMenuMapper, never()).deleteByRoleId(any());
    }

    @Test
    void deleteRoleWithUsersThrows() {
        when(roleMapper.selectOneById(1L)).thenReturn(role(1L, "operator"));
        when(adminUserRoleMapper.selectListByRoleId(1L)).thenReturn(List.of(new SysAdminUserRole()));

        ServiceException ex = assertThrows(ServiceException.class, () -> adminRoleService.deleteRole(1L));

        assertEquals(1001102, ex.getCode());
    }

    @Test
    void deleteRoleCascadesMenuRelations() {
        when(roleMapper.selectOneById(1L)).thenReturn(role(1L, "operator"));
        when(adminUserRoleMapper.selectListByRoleId(1L)).thenReturn(List.of());

        adminRoleService.deleteRole(1L);

        verify(roleMapper).deleteById(1L);
        verify(roleMenuMapper).deleteByRoleId(1L);
    }

    @Test
    void getRoleFillsMenuIds() {
        when(roleMapper.selectOneById(1L)).thenReturn(role(1L, "operator"));
        when(roleMenuMapper.selectListByRoleId(1L))
                .thenReturn(List.of(roleMenu(1L, 10L), roleMenu(1L, 11L)));
        AdminRoleVO vo = new AdminRoleVO();
        doReturn(vo).when(converter).<Object, AdminRoleVO>convert(ArgumentMatchers.<Object>any(), eq(AdminRoleVO.class));

        AdminRoleVO result = adminRoleService.getRole(1L);

        assertNotNull(result);
        assertEquals(List.of(10L, 11L), result.getMenuIds());
    }

    @Test
    void assignDataScopeCustomPersistsDeptIds() {
        when(roleMapper.selectOneById(1L)).thenReturn(role(1L, "operator"));
        AdminRoleAssignDataScopeParam param = new AdminRoleAssignDataScopeParam();
        param.setRoleId(1L);
        param.setDataScope(5);
        param.setDataScopeDeptIds(List.of(1L, 2L, 3L));

        adminRoleService.assignDataScope(param);

        ArgumentCaptor<SysRole> captor = ArgumentCaptor.forClass(SysRole.class);
        verify(roleMapper).update(captor.capture());
        assertEquals(5, captor.getValue().getDataScope());
        assertEquals("1,2,3", captor.getValue().getDataScopeDeptIds());
    }

    @Test
    void assignDataScopeNonCustomClearsDeptIds() {
        when(roleMapper.selectOneById(1L)).thenReturn(role(1L, "operator"));
        AdminRoleAssignDataScopeParam param = new AdminRoleAssignDataScopeParam();
        param.setRoleId(1L);
        param.setDataScope(1);
        param.setDataScopeDeptIds(List.of(1L, 2L));

        adminRoleService.assignDataScope(param);

        ArgumentCaptor<SysRole> captor = ArgumentCaptor.forClass(SysRole.class);
        verify(roleMapper).update(captor.capture());
        assertEquals(1, captor.getValue().getDataScope());
        assertEquals("", captor.getValue().getDataScopeDeptIds());
    }

    private SysMenu menuOf(Long id) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        return menu;
    }

    private SysRoleMenu roleMenu(Long roleId, Long menuId) {
        SysRoleMenu roleMenu = new SysRoleMenu();
        roleMenu.setRoleId(roleId);
        roleMenu.setMenuId(menuId);
        return roleMenu;
    }
}
