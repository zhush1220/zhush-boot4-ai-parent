package com.zhush.system.security;

import com.zhush.data.permission.core.DataScope;
import com.zhush.data.permission.core.DataScopeType;
import com.zhush.satoken.core.StpAdminUtil;
import com.zhush.system.entity.SysAdminUser;
import com.zhush.system.entity.SysRole;
import com.zhush.system.mapper.SysAdminUserMapper;
import com.zhush.system.mapper.SysDataResourceMapper;
import com.zhush.system.mapper.SysRoleDataResourceMapper;
import com.zhush.system.service.AdminDeptService;
import com.zhush.system.service.AdminPermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static com.zhush.system.service.AdminPermissionService.SUPER_ADMIN_CODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 管理端数据范围提供者测试：data_scope 解析、多角色并集、自定义部门串解析、缓存与主动失效
 */
@ExtendWith(MockitoExtension.class)
class AdminDataScopeProviderTest {

    @Mock
    private AdminPermissionService permissionService;
    @Mock
    private AdminDeptService deptService;
    @Mock
    private SysAdminUserMapper adminUserMapper;
    @Mock
    private SysRoleDataResourceMapper roleDataResourceMapper;
    @Mock
    private SysDataResourceMapper dataResourceMapper;

    private AdminDataScopeProvider provider;

    @BeforeEach
    void setUp() {
        provider = new AdminDataScopeProvider(permissionService, deptService, adminUserMapper,
                roleDataResourceMapper, dataResourceMapper);
    }

    private SysRole role(String code, Integer dataScope, String dataScopeDeptIds) {
        SysRole role = new SysRole();
        role.setCode(code);
        role.setDataScope(dataScope);
        role.setDataScopeDeptIds(dataScopeDeptIds);
        return role;
    }

    private SysAdminUser userOfDept(Long deptId) {
        SysAdminUser user = new SysAdminUser();
        user.setDeptId(deptId);
        return user;
    }

    @Test
    void notLoginSkipsFilter() {
        try (MockedStatic<StpAdminUtil> stp = mockStatic(StpAdminUtil.class)) {
            stp.when(StpAdminUtil::getLoginIdDefaultNull).thenReturn(null);

            DataScope scope = provider.getDataScope();

            assertEquals(DataScopeType.ALL, scope.getType());
            assertNull(scope.getUserId());
        }
        // 未登录不查角色，定时任务等内部调用不受行级过滤影响
        verifyNoInteractions(permissionService);
    }

    @Test
    void superAdminSeesAll() {
        when(permissionService.listEnabledRoles(1L))
                .thenReturn(List.of(role(SUPER_ADMIN_CODE, 4, null)));

        try (MockedStatic<StpAdminUtil> stp = mockStatic(StpAdminUtil.class)) {
            stp.when(StpAdminUtil::getLoginIdDefaultNull).thenReturn(1L);

            assertEquals(DataScopeType.ALL, provider.getDataScope().getType());
        }
    }

    @Test
    void nullDataScopeTreatedAsAll() {
        when(permissionService.listEnabledRoles(1L)).thenReturn(List.of(role("operator", null, null)));

        try (MockedStatic<StpAdminUtil> stp = mockStatic(StpAdminUtil.class)) {
            stp.when(StpAdminUtil::getLoginIdDefaultNull).thenReturn(1L);

            // 存量角色未配置 data_scope，兼容为全部
            assertEquals(DataScopeType.ALL, provider.getDataScope().getType());
        }
    }

    @Test
    void customScopeParsesCommaSeparatedDeptIds() {
        when(permissionService.listEnabledRoles(1L))
                .thenReturn(List.of(role("operator", 5, "101, 102,101")));

        try (MockedStatic<StpAdminUtil> stp = mockStatic(StpAdminUtil.class)) {
            stp.when(StpAdminUtil::getLoginIdDefaultNull).thenReturn(1L);

            DataScope scope = provider.getDataScope();

            // 逗号串还原为去重部门集合，同时保留 userId 供部门集合为空时兜底
            assertEquals(DataScopeType.CUSTOM, scope.getType());
            assertEquals(Set.of(101L, 102L), scope.getDeptIds());
            assertEquals(1L, scope.getUserId());
        }
    }

    @Test
    void multipleRolesUnionDeptIds() {
        when(permissionService.listEnabledRoles(1L))
                .thenReturn(List.of(role("dept", 2, null), role("custom", 5, "301")));
        when(adminUserMapper.selectOneById(1L)).thenReturn(userOfDept(201L));

        try (MockedStatic<StpAdminUtil> stp = mockStatic(StpAdminUtil.class)) {
            stp.when(StpAdminUtil::getLoginIdDefaultNull).thenReturn(1L);

            DataScope scope = provider.getDataScope();

            // 本部门（201）与自定义（301）取并集
            assertEquals(DataScopeType.CUSTOM, scope.getType());
            assertEquals(Set.of(201L, 301L), scope.getDeptIds());
        }
    }

    @Test
    void deptAndChildScopeExpandsChildren() {
        when(permissionService.listEnabledRoles(1L)).thenReturn(List.of(role("manager", 3, null)));
        when(adminUserMapper.selectOneById(1L)).thenReturn(userOfDept(201L));
        when(deptService.getDeptAndChildIds(201L)).thenReturn(Set.of(201L, 202L));

        try (MockedStatic<StpAdminUtil> stp = mockStatic(StpAdminUtil.class)) {
            stp.when(StpAdminUtil::getLoginIdDefaultNull).thenReturn(1L);

            DataScope scope = provider.getDataScope();

            assertEquals(Set.of(201L, 202L), scope.getDeptIds());
        }
    }

    @Test
    void selfOnlyRoleFallsBackToSelf() {
        when(permissionService.listEnabledRoles(1L)).thenReturn(List.of(role("operator", 4, null)));

        try (MockedStatic<StpAdminUtil> stp = mockStatic(StpAdminUtil.class)) {
            stp.when(StpAdminUtil::getLoginIdDefaultNull).thenReturn(1L);

            DataScope scope = provider.getDataScope();

            assertEquals(DataScopeType.SELF, scope.getType());
            assertEquals(1L, scope.getUserId());
        }
    }

    @Test
    void withoutAnyEnabledRoleFallsBackToSelf() {
        when(permissionService.listEnabledRoles(1L)).thenReturn(List.of());

        try (MockedStatic<StpAdminUtil> stp = mockStatic(StpAdminUtil.class)) {
            stp.when(StpAdminUtil::getLoginIdDefaultNull).thenReturn(1L);

            DataScope scope = provider.getDataScope();

            // 最小权限：无角色只能看本人数据
            assertEquals(DataScopeType.SELF, scope.getType());
        }
    }

    @Test
    void cacheRecomputesAfterInvalidate() {
        when(permissionService.listEnabledRoles(1L)).thenReturn(List.of(role("operator", 4, null)));

        try (MockedStatic<StpAdminUtil> stp = mockStatic(StpAdminUtil.class)) {
            stp.when(StpAdminUtil::getLoginIdDefaultNull).thenReturn(1L);

            provider.getDataScope();
            provider.getDataScope();
            // TTL 内复用缓存，不重复解析角色
            verify(permissionService, times(1)).listEnabledRoles(1L);

            provider.invalidateAll();
            provider.getDataScope();
            // 角色数据范围调整后主动失效，下一次请求立即重算
            verify(permissionService, times(2)).listEnabledRoles(1L);

            // 失效他人缓存不影响当前用户
            provider.invalidate(2L);
            provider.getDataScope();
            verify(permissionService, times(2)).listEnabledRoles(1L);
        }
    }
}
