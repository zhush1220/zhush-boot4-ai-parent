package com.zhush.agentscope.agent;

import java.util.Collection;

/**
 * 智能体注册中心：按名称获取 / 动态注册 / 查询。
 *
 * <p>启动时收集所有 {@link AgentDefinitionProvider} 初始化注册表；支持运行时动态注册。
 * 出于防提示词注入考虑，仅注册中心内存在的 Agent 可被调用。
 *
 * @author zhush
 */
public interface AgentRegistry {

    /**
     * 按名称获取定义。
     *
     * @param name 智能体名
     * @return 定义，不存在返回 null
     */
    AgentDefinition get(String name);

    /**
     * 是否已注册。
     *
     * @param name 智能体名
     * @return true 已注册
     */
    boolean contains(String name);

    /**
     * 注册（运行时动态注册）。
     *
     * @param definition 智能体定义
     */
    void register(AgentDefinition definition);

    /**
     * 移除（管理端删除 / 停用）。
     *
     * @param name 智能体名
     */
    void remove(String name);

    /**
     * 全部已注册定义。
     *
     * @return 定义集合
     */
    Collection<AgentDefinition> all();
}
