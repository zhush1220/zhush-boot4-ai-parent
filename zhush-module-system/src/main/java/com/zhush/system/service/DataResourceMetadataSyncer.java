package com.zhush.system.service;

import com.zhush.data.permission.annotation.DataPermission;
import com.zhush.data.permission.annotation.FieldPermission;
import com.zhush.system.entity.SysDataField;
import com.zhush.system.entity.SysDataResource;
import com.zhush.system.entity.SysMenu;
import com.zhush.system.mapper.SysDataFieldMapper;
import com.zhush.system.mapper.SysDataResourceMapper;
import com.zhush.system.mapper.SysMenuMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据资源元数据同步器
 * <p>
 * 从代码注解（{@link DataPermission} / {@link FieldPermission}）反向登记/刷新数据库元数据：
 * <ul>
 *     <li>扫描所有 Spring Bean 的目标类，收集带 {@code @DataPermission(resource=..)} 的方法与其返回 VO</li>
 *     <li>幂等 upsert {@code sys_data_resource}（部门列/用户列镜像注解）</li>
 *     <li>扫描 VO 的 {@code @FieldPermission} 字段，幂等 upsert {@code sys_menu} 按钮权限点 + {@code sys_data_field}</li>
 * </ul>
 * 仅管理员在"数据资源管理"页触发（POST /sync），非启动期执行。
 *
 * @author zhush
 */
@Component
public class DataResourceMetadataSyncer {

    private static final Logger log = LoggerFactory.getLogger(DataResourceMetadataSyncer.class);

    /**
     * 字段权限按钮挂载的目录菜单编码（不存在则创建）
     */
    public static final String FIELD_MENU_DIR_PERMISSION = "system:data-field:dir";

    private final ApplicationContext applicationContext;
    private final SysDataResourceMapper dataResourceMapper;
    private final SysDataFieldMapper dataFieldMapper;
    private final SysMenuMapper menuMapper;

    public DataResourceMetadataSyncer(ApplicationContext applicationContext,
                                      SysDataResourceMapper dataResourceMapper,
                                      SysDataFieldMapper dataFieldMapper,
                                      SysMenuMapper menuMapper) {
        this.applicationContext = applicationContext;
        this.dataResourceMapper = dataResourceMapper;
        this.dataFieldMapper = dataFieldMapper;
        this.menuMapper = menuMapper;
    }

    /**
     * 执行同步，返回登记的资源与字段数量摘要
     */
    public SyncResult sync() {
        SyncResult result = new SyncResult();
        String fieldMenuDirId = ensureFieldMenuDirectory();
        for (Class<?> userClass : candidateClasses()) {
            for (Method method : userClass.getDeclaredMethods()) {
                DataPermission dp = method.getAnnotation(DataPermission.class);
                if (dp == null || !dp.enabled() || dp.resource().isBlank()) {
                    continue;
                }
                SysDataResource resource = upsertResource(dp, userClass);
                result.resourceCount++;
                Class<?> voClass = extractVoClass(method);
                if (voClass == null) {
                    continue;
                }
                for (FieldInfo fp : collectFieldPermissions(voClass)) {
                    upsertField(resource, voClass, fp, fieldMenuDirId);
                    result.fieldCount++;
                }
            }
        }
        log.info("[DataResourceMetadataSyncer] 元数据同步完成：资源 {} 个，字段 {} 个",
                result.resourceCount, result.fieldCount);
        return result;
    }

    // ================ 内部方法 ================

    private List<Class<?>> candidateClasses() {
        List<Class<?>> classes = new ArrayList<>();
        for (String name : applicationContext.getBeanDefinitionNames()) {
            try {
                Class<?> type = applicationContext.getType(name);
                if (type == null) {
                    continue;
                }
                // 剥离 CGLIB/JDK 代理，取真实目标类（注解保留在目标类方法上）
                classes.add(ClassUtils.getUserClass(type));
            } catch (RuntimeException ex) {
                log.debug("[DataResourceMetadataSyncer] 跳过不可解析 Bean {}：{}", name, ex.getMessage());
            }
        }
        return classes;
    }

    private SysDataResource upsertResource(DataPermission dp, Class<?> ownerClass) {
        SysDataResource existing = dataResourceMapper.selectByCode(dp.resource());
        if (existing != null) {
            return existing;
        }
        SysDataResource resource = new SysDataResource();
        resource.setCode(dp.resource());
        resource.setName(dp.resource());
        resource.setModule(moduleOf(dp.resource()));
        resource.setDeptColumn(dp.deptColumn());
        resource.setUserColumn(dp.userColumn());
        resource.setVoClass(ownerClass.getName());
        resource.setRowEnabled(1);
        resource.setFieldEnabled(1);
        resource.setStatus(1);
        dataResourceMapper.insert(resource);
        return resource;
    }

    private void upsertField(SysDataResource resource, Class<?> voClass, FieldInfo info, String fieldMenuDirId) {
        SysDataField existing = dataFieldMapper.selectByPermissionCode(info.permission());
        if (existing != null) {
            return;
        }
        Long menuId = ensureFieldPermissionMenu(info.permission(), info.label(), fieldMenuDirId);
        SysDataField field = new SysDataField();
        field.setResourceId(resource.getId());
        field.setResourceCode(resource.getCode());
        field.setFieldPath(info.fieldPath());
        field.setLabel(info.label());
        field.setPermissionCode(info.permission());
        field.setMenuId(menuId);
        field.setDefaultStrategy(info.strategy());
        field.setMaskValue(info.maskValue());
        field.setStatus(1);
        dataFieldMapper.insert(field);
    }

    /**
     * 幂等创建"字段权限"目录菜单，返回其ID字符串
     */
    private String ensureFieldMenuDirectory() {
        SysMenu dir = menuMapper.selectByPermission(FIELD_MENU_DIR_PERMISSION);
        if (dir != null) {
            return String.valueOf(dir.getId());
        }
        SysMenu menu = new SysMenu();
        menu.setParentId(0L);
        menu.setName("字段权限");
        menu.setType(1);
        menu.setPermission(FIELD_MENU_DIR_PERMISSION);
        menu.setPath("/data-field");
        menu.setComponent("");
        menu.setSort(900);
        menu.setVisible(0);
        menu.setStatus(1);
        menuMapper.insert(menu);
        return String.valueOf(menu.getId());
    }

    /**
     * 幂等登记字段权限按钮（type=3），返回其ID
     */
    private Long ensureFieldPermissionMenu(String permission, String label, String fieldMenuDirId) {
        SysMenu existing = menuMapper.selectByPermission(permission);
        if (existing != null) {
            return existing.getId();
        }
        SysMenu menu = new SysMenu();
        menu.setParentId(Long.valueOf(fieldMenuDirId));
        menu.setName(label);
        menu.setType(3);
        menu.setPermission(permission);
        menu.setPath("");
        menu.setComponent("");
        menu.setSort(0);
        menu.setVisible(0);
        menu.setStatus(1);
        menuMapper.insert(menu);
        return menu.getId();
    }

    /**
     * 从方法返回类型中提取 VO 类（递归解泛型：PageResult&lt;VO&gt; / List&lt;VO&gt; / VO）
     */
    static Class<?> extractVoClass(Method method) {
        return resolveVo(method.getGenericReturnType());
    }

    private static Class<?> resolveVo(Type type) {
        if (type instanceof Class<?> clazz) {
            return isVoClass(clazz) ? clazz : null;
        }
        if (type instanceof ParameterizedType pt) {
            for (Type arg : pt.getActualTypeArguments()) {
                Class<?> resolved = resolveVo(arg);
                if (resolved != null) {
                    return resolved;
                }
            }
            Class<?> raw = (Class<?>) pt.getRawType();
            return isVoClass(raw) ? raw : null;
        }
        return null;
    }

    private static boolean isVoClass(Class<?> clazz) {
        return clazz.getName().contains(".vo.") || clazz.getSimpleName().endsWith("VO");
    }

    /**
     * 收集 VO 上带 {@link FieldPermission} 的字段信息
     */
    static List<FieldInfo> collectFieldPermissions(Class<?> voClass) {
        List<FieldInfo> result = new ArrayList<>();
        for (java.lang.reflect.Field field : voClass.getDeclaredFields()) {
            FieldPermission fp = field.getAnnotation(FieldPermission.class);
            if (fp == null) {
                continue;
            }
            result.add(new FieldInfo(field.getName(), fp.value(), fp.value(),
                    fp.strategy().name(), fp.maskValue()));
        }
        return result;
    }

    private static String moduleOf(String code) {
        int idx = code.indexOf(':');
        return idx > 0 ? code.substring(0, idx) : code;
    }

    /**
     * VO 字段权限元信息
     */
    record FieldInfo(String fieldPath, String permission, String label, String strategy, String maskValue) {
    }

    /**
     * 同步结果摘要
     */
    public static class SyncResult {
        public int resourceCount;
        public int fieldCount;
    }
}
