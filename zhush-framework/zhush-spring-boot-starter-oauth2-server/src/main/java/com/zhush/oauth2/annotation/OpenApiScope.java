package com.zhush.oauth2.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 开放 API scope 声明
 *
 * <p>标注在开放 API（{@code /open/**}）的方法或类上，由 {@code OpenApiInterceptor}
 * 校验调用方令牌是否持有全部声明的 scope，不足时返回 {@code 403 insufficient_scope}。
 * 方法上的注解优先于类上的注解。
 *
 * <p>示例：{@code @OpenApiScope("userinfo")}、{@code @OpenApiScope({"order:read", "order:write"})}
 *
 * @author zhush
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OpenApiScope {

    /**
     * 所需 scope 列表（须全部满足）
     */
    String[] value();

    /**
     * 是否要求用户级令牌（授权码模式签发、携带用户上下文）。
     * {@code true} 时应用级 client_token 调用将被拒绝（403）
     */
    boolean requireUser() default false;

}
