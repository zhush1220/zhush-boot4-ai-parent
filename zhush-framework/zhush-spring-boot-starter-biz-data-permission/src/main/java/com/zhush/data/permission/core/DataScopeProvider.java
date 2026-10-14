package com.zhush.data.permission.core;

/**
 * 数据范围提供者 SPI
 * <p>
 * 由业务模块实现并注册为 Spring Bean，负责解析"当前登录用户能看哪些数据"：
 * 通常根据用户的角色配置（数据范围类型 + 部门归属）计算出 {@link DataScope}。
 * DEPT_AND_CHILD 类型需要业务侧自行展开子部门后放入 deptIds。
 * <p>
 * 未提供实现时，框架默认返回 {@link DataScope#all()}（即不做行级过滤）。
 *
 * @author zhush
 */
@FunctionalInterface
public interface DataScopeProvider {

    /**
     * 获取当前登录用户的数据范围
     *
     * @return 数据范围；返回 null 等同于全部数据
     */
    DataScope getDataScope();

    /**
     * 获取当前登录用户在指定业务对象资源上的数据范围
     * <p>
     * 默认委派给无参版本（即不区分资源，保持向后兼容）；业务侧若支持"角色 × 资源"
     * 行级覆盖，应重写本方法依据 {@code resourceCode} 读取 DB 配置后返回差异化范围。
     *
     * @param resourceCode 业务对象资源编码（{@code sys_data_resource.code}），非空
     * @return 数据范围；返回 null 等同于全部数据
     */
    default DataScope getDataScope(String resourceCode) {
        return getDataScope();
    }
}
