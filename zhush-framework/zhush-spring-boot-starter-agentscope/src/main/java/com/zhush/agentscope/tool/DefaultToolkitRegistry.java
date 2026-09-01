package com.zhush.agentscope.tool;

import com.zhush.agentscope.agent.AgentDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具注册中心默认实现：内存保存已注册工具，按 Agent 定义裁剪工具集。
 *
 * <p>工具来源两类：① 启动时扫描被 {@link AgentTool} 标注的方法；② 运行时经 {@link ToolProvider} 手动注册。
 * 裁剪遵循最小暴露原则——仅 Agent 定义中声明的工具才会被装配给 LLM。
 *
 * @author zhush
 */
public class DefaultToolkitRegistry implements ToolkitRegistry {

    private static final Logger log = LoggerFactory.getLogger(DefaultToolkitRegistry.class);

    private final Map<String, RegisteredTool> tools = new ConcurrentHashMap<>();

    @Override
    public void register(Object bean, Method method, AgentTool anno) {
        String name = (anno.name() == null || anno.name().isEmpty()) ? method.getName() : anno.name();
        RegisteredTool tool = RegisteredTool.builder()
                .name(name)
                .description(anno.description())
                .beanName(bean.getClass().getSimpleName())
                .bean(bean)
                .method(method)
                .requiresApproval(anno.sensitive())
                .groupCode("common")
                .build();
        tools.put(name, tool);
        log.debug("注册 Agent 工具 [{}] -> {}.{}", name, bean.getClass().getName(), method.getName());
    }

    @Override
    public void register(RegisteredTool tool) {
        if (tool == null || tool.getName() == null) {
            return;
        }
        tools.put(tool.getName(), tool);
    }

    @Override
    public RegisteredTool get(String name) {
        return tools.get(name);
    }

    @Override
    public List<RegisteredTool> all() {
        return new ArrayList<>(tools.values());
    }

    @Override
    public List<RegisteredTool> buildToolkit(AgentDefinition definition) {
        List<String> allowed = definition.getToolNames();
        if (allowed == null || allowed.isEmpty()) {
            return new ArrayList<>();
        }
        List<RegisteredTool> result = new ArrayList<>(allowed.size());
        for (String name : allowed) {
            RegisteredTool tool = tools.get(name);
            if (tool != null) {
                result.add(tool);
            } else {
                log.warn("Agent [{}] 声明的工具 [{}] 未注册（忽略）", definition.getName(), name);
            }
        }
        return result;
    }
}
