package com.zhush.data.permission.util;

import com.zhush.data.permission.core.DataPermissionContextHolder;

import java.util.function.Supplier;

/**
 * 数据权限工具类
 * <p>
 * 提供代码级临时跳过行级过滤的能力，用于 @DataPermission 方法内部
 * 某段逻辑需要查询全量数据的场景（如统计、内部校验）。
 *
 * @author zhush
 */
public final class DataPermissionHelper {

    private DataPermissionHelper() {
    }

    /**
     * 在忽略行级数据权限的上下文中执行（无返回值）
     */
    public static void ignore(Runnable runnable) {
        boolean original = DataPermissionContextHolder.isIgnored();
        DataPermissionContextHolder.setIgnore(true);
        try {
            runnable.run();
        } finally {
            DataPermissionContextHolder.setIgnore(original);
        }
    }

    /**
     * 在忽略行级数据权限的上下文中执行（有返回值）
     */
    public static <T> T ignore(Supplier<T> supplier) {
        boolean original = DataPermissionContextHolder.isIgnored();
        DataPermissionContextHolder.setIgnore(true);
        try {
            return supplier.get();
        } finally {
            DataPermissionContextHolder.setIgnore(original);
        }
    }
}
