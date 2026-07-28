package com.zhush.system.security;

import com.zhush.data.permission.core.PermissionChecker;
import com.zhush.satoken.core.StpAdminUtil;
import org.springframework.stereotype.Component;

/**
 * 管理端字段权限校验器（委托 Sa-Token 管理端会话判定权限标识）
 *
 * @author zhush
 */
@Component
public class AdminPermissionChecker implements PermissionChecker {

    @Override
    public boolean hasPermission(String permission) {
        // 未登录一律视为无权限（字段将被置空/脱敏）
        return StpAdminUtil.isLogin() && StpAdminUtil.hasPermission(permission);
    }

}
