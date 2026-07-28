package com.zhush.data.permission.core.aop;

import com.zhush.data.permission.annotation.DataPermission;
import com.zhush.data.permission.core.DataPermissionContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;

/**
 * 行级数据权限切面
 * <p>
 * 拦截标注 {@link DataPermission} 的方法（或所在类），在方法执行期间将注解配置
 * 压入 {@link DataPermissionContextHolder}，供 MyBatis-Flex 方言钩子读取。
 *
 * @author zhush
 */
@Aspect
public class DataPermissionAspect {

    @Around("@annotation(com.zhush.data.permission.annotation.DataPermission)"
            + " || @within(com.zhush.data.permission.annotation.DataPermission)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        DataPermission annotation = resolveAnnotation(joinPoint);
        if (annotation == null) {
            return joinPoint.proceed();
        }
        DataPermissionContextHolder.push(annotation);
        try {
            return joinPoint.proceed();
        } finally {
            DataPermissionContextHolder.pop();
        }
    }

    /**
     * 解析注解：方法上优先，其次取类上（含元注解/接口继承场景）
     */
    private DataPermission resolveAnnotation(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        DataPermission annotation = AnnotatedElementUtils.findMergedAnnotation(method, DataPermission.class);
        if (annotation == null) {
            annotation = AnnotatedElementUtils.findMergedAnnotation(joinPoint.getTarget().getClass(), DataPermission.class);
        }
        return annotation;
    }
}
