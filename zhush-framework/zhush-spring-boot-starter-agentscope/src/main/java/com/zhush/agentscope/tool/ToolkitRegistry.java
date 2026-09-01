package com.zhush.agentscope.tool;

import com.zhush.agentscope.agent.AgentDefinition;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 工具注册中心：启动时扫描 {@link AgentTool} 标注的方法 + 运行时手动注册。
 *
 * <p>按 Agent 定义裁剪工具集：只有定义 {@code toolNames} 中列出的工具才被装配，
 * 避免无关工具暴露给 LLM（提示词注入/越权风险）。
 *
 * @author zhush
 */
public interface ToolkitRegistry {

    /**
     * 注册一个被 {@link AgentTool} 标注的方法。
     *
     * @param bean    目标 Bean
     * @param method  目标方法
     * @param anno    注解
     */
    void register(Object bean, Method method, AgentTool anno);

    /**
     * 手动注册（{@link ToolProvider} 使用）。
     *
     * @param tool 工具
     */
    void register(RegisteredTool tool);

    /**
     * 按名称查询工具。
     *
     * @param name 工具名
     * @return 工具，不存在返回 null
     */
    RegisteredTool get(String name);

    /**
     * 列出全部已注册工具（供工具管理页只读展示）。
     *
     * @return 全部工具
     */
    List<RegisteredTool> all();

    /**
     * 按 Agent 定义裁剪并构建工具集。
     *
     * @param definition 智能体定义
     * @return 允许的工具（空列表表示不装配工具）
     */
    List<RegisteredTool> buildToolkit(AgentDefinition definition);
}
