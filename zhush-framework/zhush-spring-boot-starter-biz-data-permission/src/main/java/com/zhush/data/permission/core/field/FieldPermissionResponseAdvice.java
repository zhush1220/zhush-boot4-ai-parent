package com.zhush.data.permission.core.field;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.data.permission.annotation.FieldPermission;
import com.zhush.data.permission.core.PermissionChecker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字段级数据权限响应过滤器
 * <p>
 * 在响应体序列化前遍历返回数据，对标注 {@link FieldPermission} 的字段做权限判定：
 * 当前用户无对应权限标识时按注解策略置空或脱敏。支持 Result / PageResult / 集合
 * 及嵌套 VO（限制递归深度，防御环形引用）。
 *
 * @author zhush
 */
@Slf4j
@RestControllerAdvice
public class FieldPermissionResponseAdvice implements ResponseBodyAdvice<Object> {

    /**
     * 嵌套 VO 最大递归深度
     */
    private static final int MAX_DEPTH = 4;

    /**
     * 类 -> 带 @FieldPermission 注解的字段缓存（空列表表示该类无需处理）
     */
    private static final Map<Class<?>, List<Field>> ANNOTATED_FIELD_CACHE = new ConcurrentHashMap<>();

    /**
     * 类 -> 需要递归处理的嵌套字段缓存（业务对象 / 集合类型字段）
     */
    private static final Map<Class<?>, List<Field>> NESTED_FIELD_CACHE = new ConcurrentHashMap<>();

    private final PermissionChecker permissionChecker;

    public FieldPermissionResponseAdvice(PermissionChecker permissionChecker) {
        this.permissionChecker = permissionChecker;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        try {
            process(body, 0);
        } catch (Exception e) {
            // 字段过滤失败不影响正常响应，仅记录告警
            log.warn("[FieldPermission] 字段级权限过滤异常", e);
        }
        return body;
    }

    private void process(Object obj, int depth) throws IllegalAccessException {
        if (obj == null || depth > MAX_DEPTH) {
            return;
        }
        if (obj instanceof Result<?> result) {
            process(result.getData(), depth);
            return;
        }
        if (obj instanceof PageResult<?> pageResult) {
            processCollection(pageResult.getList(), depth);
            return;
        }
        if (obj instanceof Collection<?> collection) {
            processCollection(collection, depth);
            return;
        }
        Class<?> clazz = obj.getClass();
        if (isPlainType(clazz) || obj instanceof Map || clazz.isArray()) {
            return;
        }
        // 处理当前对象上标注注解的字段
        for (Field field : annotatedFields(clazz)) {
            FieldPermission annotation = field.getAnnotation(FieldPermission.class);
            if (!permissionChecker.hasPermission(annotation.value())) {
                applyStrategy(obj, field, annotation);
            }
        }
        // 递归处理嵌套的业务对象 / 集合字段
        for (Field field : nestedFields(clazz)) {
            process(field.get(obj), depth + 1);
        }
    }

    private void processCollection(Collection<?> collection, int depth) throws IllegalAccessException {
        if (collection == null) {
            return;
        }
        for (Object item : collection) {
            process(item, depth);
        }
    }

    /**
     * 无权限时按策略处理字段值
     */
    private void applyStrategy(Object obj, Field field, FieldPermission annotation) throws IllegalAccessException {
        if (field.getType().isPrimitive()) {
            // 基本类型无法置空，跳过并提示改用包装类型
            log.warn("[FieldPermission] 字段 {}.{} 为基本类型，无法置空，请改用包装类型",
                    obj.getClass().getSimpleName(), field.getName());
            return;
        }
        Object newValue = annotation.strategy() == FieldPermission.Strategy.MASK
                && field.getType() == String.class ? annotation.maskValue() : null;
        field.set(obj, newValue);
    }

    /**
     * 获取类上标注 @FieldPermission 的字段（含父类），带缓存
     */
    private List<Field> annotatedFields(Class<?> clazz) {
        return ANNOTATED_FIELD_CACHE.computeIfAbsent(clazz, cls -> {
            List<Field> fields = new ArrayList<>();
            for (Field field : declaredFields(cls)) {
                if (field.isAnnotationPresent(FieldPermission.class)) {
                    field.setAccessible(true);
                    fields.add(field);
                }
            }
            return fields;
        });
    }

    /**
     * 获取需要递归处理的嵌套字段：业务对象或集合类型（含父类），带缓存
     */
    private List<Field> nestedFields(Class<?> clazz) {
        return NESTED_FIELD_CACHE.computeIfAbsent(clazz, cls -> {
            List<Field> fields = new ArrayList<>();
            for (Field field : declaredFields(cls)) {
                Class<?> type = field.getType();
                if (Collection.class.isAssignableFrom(type) || !isPlainType(type)) {
                    field.setAccessible(true);
                    fields.add(field);
                }
            }
            return fields;
        });
    }

    /**
     * 收集类及其父类的所有非静态字段
     */
    private List<Field> declaredFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    fields.add(field);
                }
            }
        }
        return fields;
    }

    /**
     * 是否为无需处理的普通类型（基本类型 / JDK 类型 / 枚举）
     */
    private boolean isPlainType(Class<?> clazz) {
        return clazz.isPrimitive() || clazz.isEnum()
                || clazz.getName().startsWith("java.") || clazz.getName().startsWith("javax.")
                || clazz.getName().startsWith("jakarta.");
    }
}
