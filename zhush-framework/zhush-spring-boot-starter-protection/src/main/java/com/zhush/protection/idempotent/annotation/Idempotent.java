package com.zhush.protection.idempotent.annotation;

import com.zhush.protection.idempotent.enums.IdempotentDimension;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 接口幂等性注解（防重复提交）
 *
 * <p>标注在 Controller / Service 方法上，由幂等切面基于 Redis {@code SET NX} 实现
 * 时间窗口内的请求去重：窗口内被判定为重复的请求不会执行业务逻辑，
 * 直接返回 {@code GlobalErrorCode.REPEATED_REQUEST}（1000409）业务错误。
 *
 * <p>去重 key 由「接口标识 + {@link #dimension()} 决定的附加信息」组成，
 * 默认参数维度（接口 + 用户 + 参数指纹），可按需切换为用户维度或接口维度。
 *
 * <p>业务方法抛异常（提交失败）时默认会清除幂等标记，允许用户修正后重新提交；
 * 如需“失败也不允许重试”（如扣款类严格一次性操作），可将 {@link #clearOnFailure()} 设为 {@code false}。
 *
 * @author zhush
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * 幂等时间窗口长度，窗口内的重复请求会被拦截
     */
    long timeout() default 5L;

    /**
     * 时间窗口单位，默认秒
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 去重维度：参数（默认）/ 用户 / 接口
     */
    IdempotentDimension dimension() default IdempotentDimension.ARGS;

    /**
     * 自定义业务 key 后缀，用于区分同一方法的不同业务场景；
     * 支持从方法入参中提取占位符，如 {@code "#param.orderId"}（简单属性路径，暂不支持复杂 SpEL）
     */
    String key() default "";

    /**
     * 重复请求时的提示文案
     */
    String message() default "请勿重复提交，请稍后再试";

    /**
     * 业务执行失败（抛异常）时是否清除幂等标记，允许重新提交；默认 {@code true}
     */
    boolean clearOnFailure() default true;

}
