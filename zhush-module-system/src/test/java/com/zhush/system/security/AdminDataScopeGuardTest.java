package com.zhush.system.security;

import com.zhush.common.exception.ServiceException;
import com.zhush.data.permission.core.DataScope;
import com.zhush.data.permission.core.DataScopeType;
import com.zhush.system.enums.SystemErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * byId 越权护栏测试：纯判定 isVisible 的各数据范围口径 + assertVisible 越权抛错。
 */
@ExtendWith(MockitoExtension.class)
class AdminDataScopeGuardTest {

    @Mock
    private AdminDataScopeProvider dataScopeProvider;
    @InjectMocks
    private AdminDataScopeGuard guard;

    @Test
    void nullScopeIsVisible() {
        assertTrue(AdminDataScopeGuard.isVisible(null, 1L, 2L));
    }

    @Test
    void allScopeIsAlwaysVisible() {
        assertTrue(AdminDataScopeGuard.isVisible(DataScope.all(), 9L, 9L));
    }

    @Test
    void selfScopeOnlyOwnerVisible() {
        DataScope self = DataScope.self(7L);
        assertTrue(AdminDataScopeGuard.isVisible(self, 1L, 7L));
        assertFalse(AdminDataScopeGuard.isVisible(self, 1L, 8L));
    }

    @Test
    void deptScopeMatchesDeptId() {
        DataScope dept = new DataScope();
        dept.setType(DataScopeType.DEPT);
        dept.setDeptIds(Set.of(100L, 200L));
        dept.setUserId(7L);
        assertTrue(AdminDataScopeGuard.isVisible(dept, 100L, 7L));
        assertFalse(AdminDataScopeGuard.isVisible(dept, 300L, 7L));
    }

    @Test
    void emptyDeptSetDegradesToSelf() {
        DataScope dept = new DataScope();
        dept.setType(DataScopeType.DEPT);
        dept.setDeptIds(Set.of());
        dept.setUserId(7L);
        // 部门集合为空退化仅本人
        assertTrue(AdminDataScopeGuard.isVisible(dept, 100L, 7L));
        assertFalse(AdminDataScopeGuard.isVisible(dept, 100L, 8L));
    }

    @Test
    void assertVisiblePassesWhenInScope() {
        when(dataScopeProvider.getDataScope()).thenReturn(DataScope.all());

        guard.assertVisible(1L, 2L, SystemErrorCode.USER_NOT_FOUND, 2L);
    }

    @Test
    void assertVisibleThrowsDenyCodeWhenOutOfScope() {
        when(dataScopeProvider.getDataScope()).thenReturn(DataScope.self(7L));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> guard.assertVisible(1L, 99L, SystemErrorCode.USER_NOT_FOUND, 99L));

        assertEquals(SystemErrorCode.USER_NOT_FOUND.getCode(), ex.getCode());
    }

}
