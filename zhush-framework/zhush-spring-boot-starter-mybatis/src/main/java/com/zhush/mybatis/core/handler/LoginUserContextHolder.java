package com.zhush.mybatis.core.handler;

import java.util.function.Supplier;

/**
 * 登录用户上下文持有者
 * <p>
 * 提供当前登录用户ID的获取能力，通过 {@link Supplier} 解耦具体认证框架。
 * 启动时由 AutoConfiguration 注入实际的 userId 获取逻辑（如 Sa-Token 的 StpUtil.getLoginIdAsLong()）。
 *
 * @author zhush
 * @since 2026/7/13
 */
public class LoginUserContextHolder {

    private static volatile Supplier<Long> userIdSupplier = () -> null;

    private LoginUserContextHolder() {
    }

    /**
     * 设置用户ID获取器（由框架启动时调用）
     *
     * @param supplier 用户ID提供者
     */
    public static void setUserIdSupplier(Supplier<Long> supplier) {
        if (supplier != null) {
            userIdSupplier = supplier;
        }
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 当前用户ID，未登录时返回 null
     */
    public static Long getUserId() {
        try {
            return userIdSupplier.get();
        } catch (Exception e) {
            return null;
        }
    }
}
