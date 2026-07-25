package com.zhush.web.operatelog.annotation;

import com.zhush.web.operatelog.enums.OperateTypeEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解
 *
 * <p>标注在 Controller 方法上，由 {@code OperateLogAspect} 切面拦截，
 * 记录本次操作的完整过程：操作人、请求信息（IP/UA/URL）、目标方法、入参、
 * 返回结果、异常信息、开始时间与执行耗时，并交由
 * {@link com.zhush.web.operatelog.core.OperateLogHandler} 落库/上报。
 *
 * <p>使用示例：
 * <pre>{@code
 * @PostMapping
 * @OperateLog(module = "用户管理", name = "新增管理员", type = OperateTypeEnum.CREATE)
 * public Result<Long> create(@RequestBody @Valid AdminUserParam param) { ... }
 * }</pre>
 *
 * @author zhush
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperateLog {

    /**
     * 所属模块（如 "用户管理"），为空时尝试取类上 Swagger {@code @Tag} 的 name
     */
    String module() default "";

    /**
     * 操作名称（如 "新增管理员"），为空时尝试取方法上 Swagger {@code @Operation} 的 summary
     */
    String name() default "";

    /**
     * 操作类型
     */
    OperateTypeEnum type() default OperateTypeEnum.OTHER;

    /**
     * 是否记录方法入参
     */
    boolean recordParams() default true;

    /**
     * 是否记录返回结果
     */
    boolean recordResult() default true;

}
