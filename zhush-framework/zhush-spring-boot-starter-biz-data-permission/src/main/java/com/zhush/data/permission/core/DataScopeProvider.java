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
}
