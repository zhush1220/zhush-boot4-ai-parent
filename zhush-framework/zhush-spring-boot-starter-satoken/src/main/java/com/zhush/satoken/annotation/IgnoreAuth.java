package com.zhush.satoken.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 忽略登录认证
 *
 * <p>标注在 Controller 类或方法上，命中的接口将跳过 Sa-Token 的登录态校验
 * （如登录、注册、验证码等匿名接口）。
 *
 * @author zhush
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IgnoreAuth {
}
