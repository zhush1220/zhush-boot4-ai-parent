package com.zhush.data.permission.core;

/**
 * 字段权限校验器 SPI
 * <p>
 * 由业务模块实现并注册为 Spring Bean，判定当前登录用户是否拥有某个权限标识
 * （通常委托给 Sa-Token：{@code StpAdminUtil.hasPermission(permission)}）。
 * <p>
 * 未提供实现时，框架默认全部放行（即字段级权限不生效）。
 *
 * @author zhush
 */
@FunctionalInterface
public interface PermissionChecker {

    /**
     * 当前登录用户是否拥有指定权限标识
     *
     * @param permission 权限标识，如 system:admin-user:mobile
     * @return true 有权限（字段原样返回）；false 无权限（字段被置空/脱敏）
     */
    boolean hasPermission(String permission);
}
