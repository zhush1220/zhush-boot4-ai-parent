package com.zhush.data.permission.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 行级数据权限注解
 * <p>
 * 标注在 Controller / Service 方法或类上，方法执行期间触发的 SQL（SELECT / UPDATE / DELETE）
 * 会按当前用户的数据范围（{@link com.zhush.data.permission.core.DataScope}）自动追加过滤条件：
 * <ul>
 *     <li>仅本人：{@code userColumn = 当前用户ID}</li>
 *     <li>本部门 / 本部门及以下 / 自定义部门：{@code deptColumn IN (部门ID集合)}</li>
 *     <li>全部数据：不追加任何条件</li>
 * </ul>
 * 未标注该注解的方法不做任何过滤；标注了但需要临时跳过时可用
 * {@link com.zhush.data.permission.util.DataPermissionHelper#ignore}。
 *
 * @author zhush
 */
@Documented
@Inherited
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataPermission {

    /**
     * 是否启用行级过滤（设为 false 可对单个方法关闭，覆盖类上的注解）
     */
    boolean enabled() default true;

    /**
     * 业务对象资源编码（对应 {@code sys_data_resource.code}）。
     * <p>
     * 为空时按"角色全局数据范围"解析（向后兼容）；非空时业务侧可依据该资源加载
     * "角色 × 资源"的行级覆盖规则，实现按业务对象分别设置数据范围。
     */
    String resource() default "";

    /**
     * 部门归属列名（表中标记数据归属部门的字段）
     */
    String deptColumn() default "dept_id";

    /**
     * 用户归属列名（表中标记数据创建人 / 归属人的字段）
     */
    String userColumn() default "create_by";
}
