package com.zhush.system.security;

import com.zhush.satoken.core.StpAdminUtil;
import com.zhush.system.service.AdminPermissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * 字段权限解析器测试：DB 权限点解析、超管通配、未登录拒绝、命中/未命中判定
 */
@ExtendWith(MockitoExtension.class)
class FieldPermissionResolverTest {

    @Mock
    private AdminPermissionService permissionService;
    @InjectMocks
    private FieldPermissionResolver resolver;

    @Test
    void resolvePermissionsDelegatesToDbService() {
        when(permissionService.getPermissionList(1L)).thenReturn(List.of("system:admin-user:mobile", "union:order:view"));

        Set<String> codes = resolver.resolvePermissions(1L);

        assertEquals(2, codes.size());
        assertTrue(codes.contains("system:admin-user:mobile"));
    }

    @Test
    void nullUserResolvesEmptySet() {
        assertTrue(resolver.resolvePermissions(null).isEmpty());
    }

    @Test
    void notLoggedInHasNoPermission() {
        try (MockedStatic<StpAdminUtil> stp = mockStatic(StpAdminUtil.class)) {
            stp.when(StpAdminUtil::getLoginIdDefaultNull).thenReturn(null);

            assertFalse(resolver.hasPermission("system:admin-user:mobile"));
        }
    }

    @Test
    void grantedPermissionIsVisible() {
        try (MockedStatic<StpAdminUtil> stp = mockStatic(StpAdminUtil.class)) {
            stp.when(StpAdminUtil::getLoginIdDefaultNull).thenReturn(1L);
            when(permissionService.getPermissionList(1L)).thenReturn(List.of("system:admin-user:mobile"));

            assertTrue(resolver.hasPermission("system:admin-user:mobile"));
            assertFalse(resolver.hasPermission("system:admin-user:id-card"));
        }
    }

    @Test
    void superAdminWildcardGrantsEveryField() {
        try (MockedStatic<StpAdminUtil> stp = mockStatic(StpAdminUtil.class)) {
            stp.when(StpAdminUtil::getLoginIdDefaultNull).thenReturn(1L);
            when(permissionService.getPermissionList(1L)).thenReturn(List.of("*"));

            assertTrue(resolver.hasPermission("system:admin-user:mobile"));
            assertTrue(resolver.hasPermission("any:other:field"));
        }
    }

}
