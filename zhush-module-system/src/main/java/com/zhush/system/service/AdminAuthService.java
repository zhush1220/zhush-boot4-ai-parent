package com.zhush.system.service;

import cn.hutool.crypto.digest.BCrypt;
import com.zhush.captcha.core.CaptchaService;
import com.zhush.common.exception.ServiceException;
import com.zhush.satoken.core.DeviceTypeConstants;
import com.zhush.satoken.core.StpAdminUtil;
import com.zhush.system.entity.SysAdminUser;
import com.zhush.system.enums.SystemErrorCode;
import com.zhush.system.mapper.SysAdminUserMapper;
import com.zhush.system.param.AdminLoginParam;
import com.zhush.system.param.AdminSocialLoginParam;
import com.zhush.system.vo.AdminLoginVO;
import com.zhush.system.vo.AdminUserInfoVO;
import com.zhush.system.vo.AdminUserMenuVO;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;
import static com.zhush.common.exception.ServiceExceptionUtil.assertTrue;
import static com.zhush.common.exception.ServiceExceptionUtil.exception;

/**
 * 管理端认证服务
 *
 * <p>账号密码登录，并预留第三方登录（如企业微信）扩展。多端登录时同端互踢：
 * 登录前先注销该账号在同一设备类型上的旧会话。
 *
 * @author zhush
 */
@Service
public class AdminAuthService {

    private static final int STATUS_ENABLED = 1;

    private final SysAdminUserMapper adminUserMapper;
    private final AdminPermissionService permissionService;
    private final LoginLogService loginLogService;
    private final RefreshTokenService refreshTokenService;
    private final AdminMenuService menuService;
    /**
     * 验证码服务（可选：zhush.captcha.enabled=false 时不装配，登录自动跳过验证码校验）
     */
    private final CaptchaService captchaService;

    public AdminAuthService(SysAdminUserMapper adminUserMapper,
                            AdminPermissionService permissionService,
                            LoginLogService loginLogService,
                            RefreshTokenService refreshTokenService,
                            AdminMenuService menuService,
                            ObjectProvider<CaptchaService> captchaServiceProvider) {
        this.adminUserMapper = adminUserMapper;
        this.permissionService = permissionService;
        this.loginLogService = loginLogService;
        this.refreshTokenService = refreshTokenService;
        this.menuService = menuService;
        this.captchaService = captchaServiceProvider.getIfAvailable();
    }

    /**
     * 账号密码登录（成功 / 失败均记录登录日志）
     */
    public AdminLoginVO login(AdminLoginParam param, String clientIp, String userAgent) {
        String device = resolveDevice(param.getDevice());
        try {
            // 验证码前置校验（一次性，防自动化爆破；验证码开关关闭时跳过）
            validateCaptcha(param.getCaptchaId(), param.getCaptchaData());

            SysAdminUser user = adminUserMapper.selectByUsername(param.getUsername());
            // 账号不存在与密码错误统一提示，避免账号探测
            assertNotNull(user, SystemErrorCode.PASSWORD_INCORRECT);
            assertTrue(BCrypt.checkpw(param.getPassword(), user.getPassword()),
                    SystemErrorCode.PASSWORD_INCORRECT);
            assertTrue(user.getStatus() != null && user.getStatus() == STATUS_ENABLED,
                    SystemErrorCode.USER_DISABLED);

            AdminLoginVO loginVO = doLogin(user, device, clientIp);
            // 登录日志：记录成功（异步落库，不阻塞登录主流程）
            loginLogService.recordLoginSuccess(user.getId(), user.getUsername(), clientIp, userAgent, device);
            return loginVO;
        } catch (ServiceException ex) {
            // 登录日志：记录失败原因（验证码错误 / 账号或密码错误 / 账号禁用等）
            loginLogService.recordLoginFailure(param.getUsername(), clientIp, userAgent, device,
                    ex.getCode(), ex.getMessage());
            throw ex;
        }
    }

    /**
     * 第三方登录（预留：企业微信、钉钉等）
     *
     * <p>完整流程：{@code code} → 调用第三方接口换取 openId → 匹配 sys_social_user 绑定 →
     * 命中则登录对应管理员。此处三方 SDK 调用尚未接入，作为扩展点抛出提示。
     */
    public AdminLoginVO socialLogin(AdminSocialLoginParam loginParam, String clientIp) {
        throw exception(SystemErrorCode.SYSTEM_CONFIG_MISSING,
                "第三方登录[" + loginParam.getSocialType() + "]");
    }

    /**
     * 刷新令牌（双 Token 无感续期）
     *
     * <p>refresh-token 单次消费：无论成功失败立即作废，防重放；
     * 成功后签发新 access-token 与新 refresh-token（轮换）。
     * 刷新不算一次登录，不记登录日志、不踢同端旧会话。
     *
     * @param refreshToken 刷新令牌（可为空，空时直接返回 null）
     * @return 新登录凭证；令牌无效 / 用户不存在 / 已禁用返回 {@code null}
     */
    public AdminLoginVO refresh(String refreshToken) {
        RefreshTokenService.Payload payload = refreshTokenService.consume(refreshToken);
        if (payload == null) {
            return null;
        }
        SysAdminUser user = adminUserMapper.selectOneById(payload.userId());
        if (user == null || user.getStatus() == null || user.getStatus() != STATUS_ENABLED) {
            return null;
        }
        // 重新登录签发新 access-token（沿用原 device，不算新登录，不做同端互踢）
        StpAdminUtil.login(user.getId(), payload.device());

        AdminLoginVO loginVO = new AdminLoginVO();
        loginVO.setUserId(user.getId());
        loginVO.setTokenName(StpAdminUtil.getTokenInfo().getTokenName());
        loginVO.setTokenValue(StpAdminUtil.getTokenValue());
        loginVO.setAccessToken(StpAdminUtil.getTokenValue());
        loginVO.setRefreshToken(refreshTokenService.issue(user.getId(), payload.device()));
        loginVO.setNickname(user.getNickname());
        return loginVO;
    }

    /**
     * 注销当前登录（双端清理：作废 refresh-token + 注销 access-token）
     */
    public void logout(String refreshToken) {
        refreshTokenService.invalidate(refreshToken);
        StpAdminUtil.logout();
    }

    /**
     * 当前登录用户信息（含角色与权限）
     */
    public AdminUserInfoVO getUserInfo() {
        Long userId = StpAdminUtil.getLoginIdAsLong();
        SysAdminUser user = adminUserMapper.selectOneById(userId);
        assertNotNull(user, SystemErrorCode.USER_NOT_FOUND, userId);

        AdminUserInfoVO vo = new AdminUserInfoVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRoles(permissionService.getRoleCodeList(userId));
        vo.setPermissions(permissionService.getPermissionList(userId));
        vo.setHomePath(menuService.getHomePath(userId));
        return vo;
    }

    /**
     * 当前登录用户的菜单路由树（按角色过滤；无权限返回空数组）
     */
    public List<AdminUserMenuVO> getUserMenus() {
        return menuService.getUserMenuTree(StpAdminUtil.getLoginIdAsLong());
    }

    /**
     * 当前登录用户的权限标识列表（用于前端按钮级鉴权）
     */
    public List<String> getAccessCodes() {
        return permissionService.getPermissionList(StpAdminUtil.getLoginIdAsLong());
    }

    // ================ 内部方法 ================

    private void validateCaptcha(String captchaId, String captchaData) {
        if (captchaService == null || !captchaService.isLoginRequired()) {
            return;
        }
        assertTrue(captchaService.validate(captchaId, captchaData),
                SystemErrorCode.CAPTCHA_INVALID);
    }

    private AdminLoginVO doLogin(SysAdminUser user, String device, String clientIp) {
        // 同端互踢：先注销该账号在同一设备类型的旧会话，再登录
        StpAdminUtil.logout(user.getId(), device);
        StpAdminUtil.login(user.getId(), device);

        // 更新登录信息
        SysAdminUser update = new SysAdminUser();
        update.setId(user.getId());
        update.setLoginIp(clientIp);
        update.setLoginDate(LocalDateTime.now());
        adminUserMapper.update(update);

        AdminLoginVO loginVO = new AdminLoginVO();
        loginVO.setUserId(user.getId());
        loginVO.setTokenName(StpAdminUtil.getTokenInfo().getTokenName());
        loginVO.setTokenValue(StpAdminUtil.getTokenValue());
        loginVO.setAccessToken(StpAdminUtil.getTokenValue());
        loginVO.setRefreshToken(refreshTokenService.issue(user.getId(), device));
        loginVO.setNickname(user.getNickname());
        return loginVO;
    }

    private String resolveDevice(String device) {
        if (device == null || device.isBlank()) {
            return DeviceTypeConstants.PC;
        }
        return device.trim().toUpperCase();
    }

}
