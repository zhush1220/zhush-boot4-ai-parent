package com.zhush.system.security;

import cn.hutool.crypto.digest.BCrypt;
import com.zhush.common.response.Result;
import com.zhush.oauth2.core.OAuth2DoLoginHandler;
import com.zhush.satoken.core.DeviceTypeConstants;
import com.zhush.satoken.core.StpAdminUtil;
import com.zhush.system.entity.SysAdminUser;
import com.zhush.system.enums.SystemErrorCode;
import com.zhush.system.mapper.SysAdminUserMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;
import static com.zhush.common.exception.ServiceExceptionUtil.assertTrue;

/**
 * OAuth2 授权页登录处理器（管理端账号体系）
 *
 * <p>用户在统一授权页提交账号密码（{@code POST /oauth2/doLogin}）时回调，
 * 校验逻辑对齐 {@code AdminAuthService#login}：BCrypt 密码比对 + 启用状态校验，
 * 通过后走 {@code StpAdminUtil} 建立会话，授权码流程随即可读取登录态。
 *
 * @author zhush
 */
@Component
public class Oauth2DoLoginHandlerImpl implements OAuth2DoLoginHandler {

    private static final int STATUS_ENABLED = 1;

    private final SysAdminUserMapper adminUserMapper;

    public Oauth2DoLoginHandlerImpl(SysAdminUserMapper adminUserMapper) {
        this.adminUserMapper = adminUserMapper;
    }

    @Override
    public Object doLogin(String username, String password) {
        SysAdminUser user = adminUserMapper.selectByUsername(username);
        // 账号不存在与密码错误统一提示，避免账号探测
        assertNotNull(user, SystemErrorCode.PASSWORD_INCORRECT);
        assertTrue(BCrypt.checkpw(password, user.getPassword()),
                SystemErrorCode.PASSWORD_INCORRECT);
        assertTrue(user.getStatus() != null && user.getStatus() == STATUS_ENABLED,
                SystemErrorCode.USER_DISABLED);

        StpAdminUtil.login(user.getId(), DeviceTypeConstants.PC);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", user.getId());
        data.put("tokenName", StpAdminUtil.getTokenInfo().getTokenName());
        data.put("tokenValue", StpAdminUtil.getTokenValue());
        return Result.success(data);
    }

}
