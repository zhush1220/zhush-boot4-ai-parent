package com.zhush.system.service;

import cn.hutool.crypto.digest.BCrypt;
import com.zhush.common.exception.ServiceException;
import com.zhush.system.enums.SystemErrorCode;
import com.zhush.system.entity.SysAdminUser;
import com.zhush.system.entity.SysAdminUserRole;
import com.zhush.system.entity.SysRole;
import com.zhush.system.mapper.SysAdminUserMapper;
import com.zhush.system.mapper.SysAdminUserRoleMapper;
import com.zhush.system.mapper.SysRoleMapper;
import com.zhush.system.param.AdminUserParam;
import com.zhush.system.security.AdminDataScopeGuard;
import com.zhush.system.security.AdminDataScopeProvider;
import com.zhush.system.vo.AdminUserVO;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理员用户服务测试：用户名唯一性、BCrypt 加密、角色分配过滤、密码重置、数据范围缓存失效
 */
@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private SysAdminUserMapper adminUserMapper;
    @Mock
    private SysAdminUserRoleMapper adminUserRoleMapper;
    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private Converter converter;
    @Mock
    private AdminDataScopeProvider dataScopeProvider;
    @Mock
    private AdminDataScopeGuard dataScopeGuard;
    @InjectMocks
    private AdminUserService adminUserService;

    private AdminUserParam param(Long id, String username, String password) {
        AdminUserParam param = new AdminUserParam();
        param.setId(id);
        param.setUsername(username);
        param.setPassword(password);
        return param;
    }

    private SysAdminUser user(Long id, String username) {
        SysAdminUser user = new SysAdminUser();
        user.setId(id);
        user.setUsername(username);
        user.setPassword("$2a$10$hashed");
        user.setNickname("管理员");
        user.setStatus(1);
        return user;
    }

    @Test
    void createUserRequiresPassword() {
        AdminUserParam param = param(null, "zhangsan", null);

        ServiceException ex = assertThrows(ServiceException.class, () -> adminUserService.createUser(param));

        assertEquals(1001006, ex.getCode());
    }

    @Test
    void createUserRejectsDuplicateUsername() {
        when(adminUserMapper.selectByUsername("zhangsan")).thenReturn(user(99L, "zhangsan"));
        AdminUserParam param = param(null, "zhangsan", "123456");

        ServiceException ex = assertThrows(ServiceException.class, () -> adminUserService.createUser(param));

        assertEquals(1001003, ex.getCode());
    }

    @Test
    void createUserEncryptsPasswordAndDefaultsEnabled() {
        SysAdminUser converted = new SysAdminUser();
        converted.setId(100L);
        converted.setUsername("zhangsan");
        doReturn(converted).when(converter).<Object, SysAdminUser>convert(ArgumentMatchers.<Object>any(), eq(SysAdminUser.class));
        when(roleMapper.selectListByIds(List.of(1L, 2L))).thenReturn(List.of(roleOf(1L)));

        AdminUserParam param = param(null, "zhangsan", "123456");
        param.setRoleIds(List.of(1L, 2L));

        Long id = adminUserService.createUser(param);

        assertEquals(100L, id);
        ArgumentCaptor<SysAdminUser> userCaptor = ArgumentCaptor.forClass(SysAdminUser.class);
        verify(adminUserMapper).insert(userCaptor.capture());
        // 密码 BCrypt 加密、状态默认启用
        assertEquals(1, userCaptor.getValue().getStatus());
        assertTrue(BCrypt.checkpw("123456", userCaptor.getValue().getPassword()));
        // 角色过滤：不存在的角色 2 被剔除，仅保存角色 1
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SysAdminUserRole>> relCaptor = ArgumentCaptor.forClass(List.class);
        verify(adminUserRoleMapper).insertBatchWithDb(relCaptor.capture());
        assertEquals(1, relCaptor.getValue().size());
        assertEquals(1L, relCaptor.getValue().get(0).getRoleId());
        assertEquals(100L, relCaptor.getValue().get(0).getUserId());
    }

    @Test
    void createUserWithEmptyRoleIdsSkipsRelationInsert() {
        SysAdminUser converted = new SysAdminUser();
        converted.setId(100L);
        doReturn(converted).when(converter).<Object, SysAdminUser>convert(ArgumentMatchers.<Object>any(), eq(SysAdminUser.class));

        AdminUserParam param = param(null, "zhangsan", "123456");

        adminUserService.createUser(param);

        verify(adminUserRoleMapper, never()).insertBatchWithDb(any());
    }

    @Test
    void updateUserNotFoundThrows() {
        when(adminUserMapper.selectOneById(1L)).thenReturn(null);
        AdminUserParam param = param(1L, "zhangsan", null);

        ServiceException ex = assertThrows(ServiceException.class, () -> adminUserService.updateUser(param));

        assertEquals(1001001, ex.getCode());
    }

    @Test
    void updateUserNeverChangesUsernameAndRehashesGivenPassword() {
        when(adminUserMapper.selectOneById(1L)).thenReturn(user(1L, "zhangsan"));
        SysAdminUser converted = new SysAdminUser();
        converted.setId(1L);
        converted.setUsername("renamed");
        converted.setPassword("newpass");
        doReturn(converted).when(converter).<Object, SysAdminUser>convert(ArgumentMatchers.<Object>any(), eq(SysAdminUser.class));

        AdminUserParam param = param(1L, "renamed", "newpass");
        param.setRoleIds(null);

        adminUserService.updateUser(param);

        ArgumentCaptor<SysAdminUser> captor = ArgumentCaptor.forClass(SysAdminUser.class);
        verify(adminUserMapper).update(captor.capture());
        // 账号不可通过修改接口变更
        assertNull(captor.getValue().getUsername());
        // 传了新密码时加密后更新
        assertTrue(BCrypt.checkpw("newpass", captor.getValue().getPassword()));
        // roleIds 为 null 时不触碰角色
        verify(adminUserRoleMapper, never()).deleteByUserId(any());
        // 归属部门可能变更，失效该用户数据范围缓存
        verify(dataScopeProvider).invalidate(1L);
    }

    @Test
    void updateUserKeepsPasswordWhenBlank() {
        when(adminUserMapper.selectOneById(1L)).thenReturn(user(1L, "zhangsan"));
        SysAdminUser converted = new SysAdminUser();
        converted.setId(1L);
        converted.setPassword("plain-from-form");
        doReturn(converted).when(converter).<Object, SysAdminUser>convert(ArgumentMatchers.<Object>any(), eq(SysAdminUser.class));

        // 修改资料不填密码：置 null 使更新忽略该列，不覆盖原密码
        adminUserService.updateUser(param(1L, "zhangsan", null));

        ArgumentCaptor<SysAdminUser> captor = ArgumentCaptor.forClass(SysAdminUser.class);
        verify(adminUserMapper).update(captor.capture());
        assertNull(captor.getValue().getPassword());
    }

    @Test
    void updateUserWithRoleIdsOverridesRoles() {
        when(adminUserMapper.selectOneById(1L)).thenReturn(user(1L, "zhangsan"));
        SysAdminUser converted = new SysAdminUser();
        converted.setId(1L);
        doReturn(converted).when(converter).<Object, SysAdminUser>convert(ArgumentMatchers.<Object>any(), eq(SysAdminUser.class));
        when(roleMapper.selectListByIds(List.of(9L))).thenReturn(List.of(roleOf(9L)));

        AdminUserParam param = param(1L, "zhangsan", null);
        param.setRoleIds(List.of(9L));

        adminUserService.updateUser(param);

        verify(adminUserRoleMapper).deleteByUserId(1L);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SysAdminUserRole>> relCaptor = ArgumentCaptor.forClass(List.class);
        verify(adminUserRoleMapper).insertBatchWithDb(relCaptor.capture());
        assertEquals(9L, relCaptor.getValue().get(0).getRoleId());
        // 角色变化后重算该用户数据范围
        verify(dataScopeProvider).invalidate(1L);
    }

    @Test
    void deleteUserCascadesRoleRelations() {
        when(adminUserMapper.selectOneById(1L)).thenReturn(user(1L, "zhangsan"));

        adminUserService.deleteUser(1L);

        verify(adminUserMapper).deleteById(1L);
        verify(adminUserRoleMapper).deleteByUserId(1L);
        // 用户删除后清理其数据范围缓存条目
        verify(dataScopeProvider).invalidate(1L);
    }

    @Test
    void getUserFillsRoleIds() {
        when(adminUserMapper.selectOneById(1L)).thenReturn(user(1L, "zhangsan"));
        when(adminUserRoleMapper.selectListByUserId(1L))
                .thenReturn(List.of(relation(1L, 10L), relation(1L, 11L)));
        AdminUserVO vo = new AdminUserVO();
        doReturn(vo).when(converter).<Object, AdminUserVO>convert(ArgumentMatchers.<Object>any(), eq(AdminUserVO.class));

        AdminUserVO result = adminUserService.getUser(1L);

        assertNotNull(result);
        assertEquals(List.of(10L, 11L), result.getRoleIds());
    }

    @Test
    void assignRolesReplacesExisting() {
        when(adminUserMapper.selectOneById(1L)).thenReturn(user(1L, "zhangsan"));
        when(roleMapper.selectListByIds(List.of(10L))).thenReturn(List.of(roleOf(10L)));

        adminUserService.assignRoles(1L, List.of(10L));

        verify(adminUserRoleMapper).deleteByUserId(1L);
        verify(adminUserRoleMapper).insertBatchWithDb(any());
        verify(dataScopeProvider).invalidate(1L);
    }

    @Test
    void resetPasswordEncryptsNewPassword() {
        when(adminUserMapper.selectOneById(1L)).thenReturn(user(1L, "zhangsan"));

        adminUserService.resetPassword(1L, "new-pass-123");

        ArgumentCaptor<SysAdminUser> captor = ArgumentCaptor.forClass(SysAdminUser.class);
        verify(adminUserMapper).update(captor.capture());
        assertEquals(1L, captor.getValue().getId());
        assertTrue(BCrypt.checkpw("new-pass-123", captor.getValue().getPassword()));
    }

    @Test
    void updateUserDeniedByScopeGuardThrows() {
        when(adminUserMapper.selectOneById(1L)).thenReturn(user(1L, "zhangsan"));
        // 越权护栏判定目标行不可见→抛 USER_NOT_FOUND，不进入更新
        doThrow(new ServiceException(SystemErrorCode.USER_NOT_FOUND, 1L))
                .when(dataScopeGuard).assertVisible(any(), any(), any(), any());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> adminUserService.updateUser(param(1L, "zhangsan", null)));

        assertEquals(1001001, ex.getCode());
        verify(adminUserMapper, never()).update(any());
    }

    private SysRole roleOf(Long id) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setStatus(1);
        return role;
    }

    private SysAdminUserRole relation(Long userId, Long roleId) {
        SysAdminUserRole relation = new SysAdminUserRole();
        relation.setUserId(userId);
        relation.setRoleId(roleId);
        return relation;
    }
}
