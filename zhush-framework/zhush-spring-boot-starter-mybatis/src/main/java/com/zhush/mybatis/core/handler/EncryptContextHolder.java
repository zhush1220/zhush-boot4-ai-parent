package com.zhush.mybatis.core.handler;

/**
 * 加密上下文持有者
 * <p>
 * 用于在 MyBatis TypeHandler（非 Spring Bean，由 MyBatis 实例化）中
 * 访问 Spring 管理的加密配置。通过静态变量桥接 Spring 配置和 MyBatis 类型处理器。
 * <p>
 * 由 {@link com.zhush.mybatis.config.EncryptAutoConfiguration} 在启动时初始化。
 *
 * @author zhush
 * @since 2026/7/11
 */
public final class EncryptContextHolder {

    private static volatile boolean enabled = true;

    private EncryptContextHolder() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        EncryptContextHolder.enabled = enabled;
    }
}
