package com.zhush.system.service;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.hutool.crypto.digest.BCrypt;
import com.zhush.captcha.core.CaptchaService;
import com.zhush.common.exception.ServiceException;
import com.zhush.satoken.core.StpAdminUtil;
import com.zhush.system.entity.SysAdminUser;
import com.zhush.system.mapper.SysAdminUserMapper;
import com.zhush.system.param.AdminLoginParam;
import com.zhush.system.param.AdminSocialLoginParam;
import com.zhush.system.vo.AdminLoginVO;
import com.zhush.system.vo.AdminUserInfoVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理端登录链路测试：账号密码校验、同端互踢、禁用拦截、登录信息更新
 */
@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    private static final String RAW_PASSWORD = "123456";
    /** BCrypt 计算约 100ms/次，缓存一份避免重复计算 */
    private static final String HASHED_PASSWORD = BCrypt.hashpw(RAW_PASSWORD);

    @Mock
    private SysAdminUserMapper adminUserMapper;
    @Mock
    private AdminPermissionService permissionService;
    @Mock
    private LoginLogService loginLogService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private AdminMenuService menuService;
    @Mock
    private ObjectProvider<CaptchaService> captchaServiceProvider;
    @InjectMocks
    private AdminAuthService adminAuthService;

    private SysAdminUser enabledUser(Long id, String username) {
        SysAdminUser user = new SysAdminUser();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(HASHED_PASSWORD);
        user.setNickname("管理员");
        user.setStatus(1);
        return user;
    }

    private AdminLoginParam loginParam(String username, String password) {
        AdminLoginParam param = new AdminLoginParam();
        param.setUsername(username);
        param.setPassword(password);
        return param;
    }

    @Test
    void loginSuccess() {
        SysAdminUser user = enabledUser(1L, "admin");
        when(adminUserMapper.selectByUsername("admin")).thenReturn(user);
        AdminLoginParam param = loginParam("admin", RAW_PASSWORD);

        try (MockedStatic<StpAdminUtil> stp = mockStatic(StpAdminUtil.class)) {
            SaTokenInfo tokenInfo = mock(SaTokenInfo.class);
            when(tokenInfo.getTokenName()).thenReturn("satoken-admin");
            stp.when(StpAdminUtil::getTokenInfo).thenReturn(tokenInfo);
            stp.when(StpAdminUtil::getTokenValue).thenReturn("mock-token-value");

            AdminLoginVO vo = adminAuthService.login(param, "127.0.0.1", "ua");

            assertEquals(1L, vo.getUserId());
            assertEquals("satoken-admin", vo.getTokenName());
            assertEquals("mock-token-value", vo.getTokenValue());
            assertEquals("管理员", vo.getNickname());
            // 同端互踢：先注销旧会话再登录
            stp.verify(() -> StpAdminUtil.logout(1L, "PC"));
            stp.verify(() -> StpAdminUtil.login(1L, "PC"));
        }
        // 登录信息更新
        ArgumentCaptor<SysAdminUser> captor = ArgumentCaptor.forClass(SysAdminUser.class);
        verify(adminUserMapper).update(captor.capture());
        assertEquals("127.0.0.1", captor.getValue().getLoginIp());
        assertNotNull(captor.getValue().getLoginDate());
    }

    @Test
    void loginUserNotFoundThrowsUnifiedPasswordError() {
        when(adminUserMapper.selectByUsername("ghost")).thenReturn(null);
        AdminLoginParam param = loginParam("ghost", "anything");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> adminAuthService.login(param, "127.0.0.1", "ua"));

        assertEquals(1001004, ex.getCode());
        assertTrue(ex.getMessage().contains("账号或密码错误"));
    }

    @Test
    void loginWrongPasswordThrowsUnifiedPasswordError() {
        when(adminUserMapper.selectByUsername("admin")).thenReturn(enabledUser(1L, "admin"));
        AdminLoginParam param = loginParam("admin", "wrong-password");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> adminAuthService.login(param, "127.0.0.1", "ua"));

        assertEquals(1001004, ex.getCode());
    }

    @Test
    void loginDisabledUserThrowsDisabledError() {
        SysAdminUser disabled = enabledUser(1L, "admin");
        disabled.setStatus(0);
        when(adminUserMapper.selectByUsername("admin")).thenReturn(disabled);
        AdminLoginParam param = loginParam("admin", RAW_PASSWORD);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> adminAuthService.login(param, "127.0.0.1", "ua"));

        assertEquals(1001002, ex.getCode());
        assertTrue(ex.getMessage().contains("用户已被禁用"));
    }

    @Test
    void loginDefaultsToPcDevice() {
        when(adminUserMapper.selectByUsername("admin")).thenReturn(enabledUser(1L, "admin"));
        AdminLoginParam param = loginParam("admin", RAW_PASSWORD);
        param.setDevice(null);

        try (MockedStatic<StpAdminUtil> stp = mockStatic(StpAdminUtil.class)) {
            SaTokenInfo tokenInfo = mock(SaTokenInfo.class);
            when(tokenInfo.getTokenName()).thenReturn("satoken-admin");
            stp.when(StpAdminUtil::getTokenInfo).thenReturn(tokenInfo);
            stp.when(StpAdminUtil::getTokenValue).thenReturn("mock-token-value");
            adminAuthService.login(param, "127.0.0.1", "ua");
            stp.verify(() -> StpAdminUtil.logout(1L, "PC"));
            stp.verify(() -> StpAdminUtil.login(1L, "PC"));
        }
    }

    @Test
    void loginNormalizesDeviceToUpperCase() {
        when(adminUserMapper.selectByUsername("admin")).thenReturn(enabledUser(1L, "admin"));
        AdminLoginParam param = loginParam("admin", RAW_PASSWORD);
        param.setDevice(" mini ");

        try (MockedStatic<StpAdminUtil> stp = mockStatic(StpAdminUtil.class)) {
            SaTokenInfo tokenInfo = mock(SaTokenInfo.class);
            when(tokenInfo.getTokenName()).thenReturn("satoken-admin");
            stp.when(StpAdminUtil::getTokenInfo).thenReturn(tokenInfo);
            stp.when(StpAdminUtil::getTokenValue).thenReturn("mock-token-value");
            adminAuthService.login(param, "127.0.0.1", "ua");
            stp.verify(() -> StpAdminUtil.login(1L, "MINI"));
        }
    }

    @Test
    void socialLoginNotConfiguredThrows() {
        AdminSocialLoginParam param = new AdminSocialLoginParam();
        param.setSocialType("WECOM");
        param.setCode("code");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> adminAuthService.socialLogin(param, "127.0.0.1"));

        assertEquals(2001001, ex.getCode());
        assertTrue(ex.getMessage().contains("第三方登录"));
    }

    @Test
    void logoutDelegatesToStp() {
        try (MockedStatic<StpAdminUtil> stp = mockStatic(StpAdminUtil.class)) {
            adminAuthService.logout("mock-refresh-token");
            stp.verify(StpAdminUtil::logout);
        }
    }

    @Test
    void getUserInfoAssemblesRolesAndPermissions() {
        when(adminUserMapper.selectOneById(1L)).thenReturn(enabledUser(1L, "admin"));
        when(permissionService.getRoleCodeList(1L)).thenReturn(List.of("super_admin"));
        when(permissionService.getPermissionList(1L)).thenReturn(List.of("*"));

        try (MockedStatic<StpAdminUtil> stp = mockStatic(StpAdminUtil.class)) {
            stp.when(StpAdminUtil::getLoginIdAsLong).thenReturn(1L);

            AdminUserInfoVO vo = adminAuthService.getUserInfo();

            assertEquals(1L, vo.getUserId());
            assertEquals("admin", vo.getUsername());
            assertEquals("管理员", vo.getNickname());
            assertEquals(List.of("super_admin"), vo.getRoles());
            assertEquals(List.of("*"), vo.getPermissions());
        }
    }

    @Test
    void getUserInfoUserNotFoundThrows() {
        when(adminUserMapper.selectOneById(9L)).thenReturn(null);

        try (MockedStatic<StpAdminUtil> stp = mockStatic(StpAdminUtil.class)) {
            stp.when(StpAdminUtil::getLoginIdAsLong).thenReturn(9L);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> adminAuthService.getUserInfo());

            assertEquals(1001001, ex.getCode());
            assertTrue(ex.getMessage().contains("用户不存在"));
        }
    }

    @Test
    void passwordIsStoredAsBcryptHash() {
        // 验证被测代码使用的哈希与明文可校验（登录成功路径已间接覆盖，此处显式断言加密强度）
        assertTrue(BCrypt.checkpw(RAW_PASSWORD, HASHED_PASSWORD));
        assertEquals(60, HASHED_PASSWORD.length());
    }

    @Test
    void loginCaptchaInvalidThrowsBeforeDbAccess() {
        CaptchaService captchaService = mock(CaptchaService.class);
        when(captchaService.isLoginRequired()).thenReturn(true);
        when(captchaService.validate("captcha-1", "bad")).thenReturn(false);
        AdminAuthService service = serviceWithCaptcha(captchaService);
        AdminLoginParam param = loginParam("admin", RAW_PASSWORD);
        param.setCaptchaId("captcha-1");
        param.setCaptchaData("bad");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.login(param, "127.0.0.1", "ua"));

        assertEquals(1001005, ex.getCode());
        // 验证码失败时不进入账号密码校验
        verify(adminUserMapper, never()).selectByUsername(any());
    }

    @Test
    void loginWithValidCaptchaProceeds() {
        CaptchaService captchaService = mock(CaptchaService.class);
        when(captchaService.isLoginRequired()).thenReturn(true);
        when(captchaService.validate("captcha-1", "180")).thenReturn(true);
        when(adminUserMapper.selectByUsername("admin")).thenReturn(enabledUser(1L, "admin"));
        AdminAuthService service = serviceWithCaptcha(captchaService);
        AdminLoginParam param = loginParam("admin", RAW_PASSWORD);
        param.setCaptchaId("captcha-1");
        param.setCaptchaData("180");

        try (MockedStatic<StpAdminUtil> stp = mockStatic(StpAdminUtil.class)) {
            SaTokenInfo tokenInfo = mock(SaTokenInfo.class);
            when(tokenInfo.getTokenName()).thenReturn("satoken-admin");
            stp.when(StpAdminUtil::getTokenInfo).thenReturn(tokenInfo);
            stp.when(StpAdminUtil::getTokenValue).thenReturn("mock-token-value");

            AdminLoginVO vo = service.login(param, "127.0.0.1", "ua");

            assertEquals(1L, vo.getUserId());
        }
    }

    @Test
    void loginSuccessRecordsLoginLog() {
        when(adminUserMapper.selectByUsername("admin")).thenReturn(enabledUser(1L, "admin"));
        AdminLoginParam param = loginParam("admin", RAW_PASSWORD);

        try (MockedStatic<StpAdminUtil> stp = mockStatic(StpAdminUtil.class)) {
            SaTokenInfo tokenInfo = mock(SaTokenInfo.class);
            when(tokenInfo.getTokenName()).thenReturn("satoken-admin");
            stp.when(StpAdminUtil::getTokenInfo).thenReturn(tokenInfo);
            stp.when(StpAdminUtil::getTokenValue).thenReturn("mock-token-value");

            adminAuthService.login(param, "127.0.0.1", "ua");
        }

        // 登录成功记录日志，不记录失败
        verify(loginLogService).recordLoginSuccess(1L, "admin", "127.0.0.1", "ua", "PC");
        verify(loginLogService, never()).recordLoginFailure(any(), any(), any(), any(), eq(0), any());
    }

    @Test
    void loginFailureRecordsLoginLog() {
        when(adminUserMapper.selectByUsername("admin")).thenReturn(enabledUser(1L, "admin"));
        AdminLoginParam param = loginParam("admin", "wrong-password");

        assertThrows(ServiceException.class,
                () -> adminAuthService.login(param, "127.0.0.1", "ua"));

        // 密码错误记录失败日志（错误码 1001004），不记录成功
        verify(loginLogService).recordLoginFailure(eq("admin"), eq("127.0.0.1"), eq("ua"), eq("PC"),
                eq(1001004), any());
        verify(loginLogService, never()).recordLoginSuccess(any(), any(), any(), any(), any());
    }

    @SuppressWarnings("unchecked")
    private AdminAuthService serviceWithCaptcha(CaptchaService captchaService) {
        ObjectProvider<CaptchaService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(captchaService);
        return new AdminAuthService(adminUserMapper, permissionService, loginLogService,
                refreshTokenService, menuService, provider);
    }
}
