package com.zhush.data.permission.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段级数据权限注解
 * <p>
 * 标注在响应 VO 的字段上，接口返回时若当前用户不具备 {@link #value()} 指定的权限标识，
 * 该字段会按 {@link #strategy()} 被置空或脱敏后再序列化输出。
 * <p>
 * 示例：{@code @FieldPermission("system:admin-user:mobile")} 标注手机号字段，
 * 仅拥有该权限标识的用户可以看到真实手机号。
 *
 * @author zhush
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldPermission {

    /**
     * 查看该字段所需的权限标识（由 PermissionChecker 判定）
     */
    String value();

    /**
     * 无权限时的处理策略
     */
    Strategy strategy() default Strategy.CLEAR;

    /**
     * 脱敏占位内容（仅 strategy = MASK 且字段为 String 时生效）
     */
    String maskValue() default "***";

    /**
     * 无权限时的字段处理策略
     */
    enum Strategy {

        /** 置空（字段输出 null） */
        CLEAR,

        /** 脱敏（String 字段输出 maskValue，非 String 字段等同置空） */
        MASK
    }
}
