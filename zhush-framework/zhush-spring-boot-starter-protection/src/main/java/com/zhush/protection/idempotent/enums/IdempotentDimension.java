package com.zhush.protection.idempotent.enums;

/**
 * 幂等去重维度
 *
 * <p>去重 key 由「接口标识 + 维度附加信息」组成，维度决定附加信息的粒度：
 * <ul>
 *     <li>{@link #ARGS}：接口 + 当前用户 + 请求参数指纹（同一用户相同参数视为重复，默认）</li>
 *     <li>{@link #USER}：接口 + 当前用户（同一用户在时间窗口内只能调用一次，忽略参数差异）</li>
 *     <li>{@link #INTERFACE}：接口全局（时间窗口内所有用户只能调用一次，慎用）</li>
 * </ul>
 *
 * @author zhush
 */
public enum IdempotentDimension {

    /**
     * 参数维度：接口 + 用户 + 参数指纹
     */
    ARGS,

    /**
     * 用户维度：接口 + 用户
     */
    USER,

    /**
     * 接口维度：仅接口
     */
    INTERFACE

}
