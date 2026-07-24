package com.zhush.system.service;

import cn.hutool.crypto.digest.BCrypt;
import com.zhush.satoken.core.DeviceTypeConstants;
import com.zhush.satoken.core.StpAdminUtil;
import com.zhush.system.entity.SysAdminUser;
import com.zhush.system.enums.SystemErrorCode;
import com.zhush.system.mapper.SysAdminUserMapper;
import com.zhush.system.param.AdminLoginParam;
import com.zhush.system.param.AdminSocialLoginParam;
import com.zhush.system.vo.AdminLoginVO;
import com.zhush.system.vo.AdminUserInfoVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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

    public AdminAuthService(SysAdminUserMapper adminUserMapper,
                            AdminPermissionService permissionService) {
        this.adminUserMapper = adminUserMapper;
        this.permissionService = permissionService;
    }

    /**
     * 账号密码登录
     */
    public AdminLoginVO login(AdminLoginParam param, String clientIp) {
        SysAdminUser user = adminUserMapper.selectByUsername(param.getUsername());
        // 账号不存在与密码错误统一提示，避免账号探测
        assertNotNull(user, SystemErrorCode.PASSWORD_INCORRECT);
        assertTrue(BCrypt.checkpw(param.getPassword(), user.getPassword()),
                SystemErrorCode.PASSWORD_INCORRECT);
        assertTrue(user.getStatus() != null && user.getStatus() == STATUS_ENABLED,
                SystemErrorCode.USER_DISABLED);

        return doLogin(user, resolveDevice(param.getDevice()), clientIp);
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
     * 注销当前登录
     */
    public void logout() {
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
        return vo;
    }

    // ================ 内部方法 ================

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
