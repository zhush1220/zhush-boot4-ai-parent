package com.zhush.data.permission.core;

import com.zhush.data.permission.annotation.DataPermission;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 数据权限上下文持有器
 * <p>
 * 以线程栈的方式保存当前生效的 {@link DataPermission} 注解配置（支持方法嵌套调用），
 * 并维护"临时忽略"标记。方言钩子从这里读取上下文决定是否追加过滤条件。
 *
 * @author zhush
 */
public final class DataPermissionContextHolder {

    /**
     * 当前线程生效的注解栈（栈顶为最近进入的方法）
     */
    private static final ThreadLocal<Deque<DataPermission>> ANNOTATION_STACK = new ThreadLocal<>();

    /**
     * 临时忽略标记（true 时行级过滤不生效）
     */
    private static final ThreadLocal<Boolean> IGNORE = new ThreadLocal<>();

    private DataPermissionContextHolder() {
    }

    /**
     * 进入 @DataPermission 方法时压栈
     */
    public static void push(DataPermission dataPermission) {
        Deque<DataPermission> stack = ANNOTATION_STACK.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            ANNOTATION_STACK.set(stack);
        }
        stack.push(dataPermission);
    }

    /**
     * 离开 @DataPermission 方法时出栈（空栈后清理 ThreadLocal，避免线程池泄漏）
     */
    public static void pop() {
        Deque<DataPermission> stack = ANNOTATION_STACK.get();
        if (stack != null) {
            stack.poll();
            if (stack.isEmpty()) {
                ANNOTATION_STACK.remove();
            }
        }
    }

    /**
     * 获取当前生效的注解配置（未处于 @DataPermission 方法内时返回 null）
     */
    public static DataPermission peek() {
        Deque<DataPermission> stack = ANNOTATION_STACK.get();
        return stack == null ? null : stack.peek();
    }

    /**
     * 是否处于临时忽略状态
     */
    public static boolean isIgnored() {
        return Boolean.TRUE.equals(IGNORE.get());
    }

    /**
     * 设置/清除临时忽略标记
     */
    public static void setIgnore(boolean ignore) {
        if (ignore) {
            IGNORE.set(Boolean.TRUE);
        } else {
            IGNORE.remove();
        }
    }
}
