package com.zhush.agentscope.agent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 默认注册中心：启动时收集所有 {@link AgentDefinitionProvider} 初始化注册表。
 *
 * @author zhush
 */
public class DefaultAgentRegistry implements AgentRegistry {

    private final ConcurrentMap<String, AgentDefinition> definitions = new ConcurrentHashMap<>();

    public DefaultAgentRegistry(List<AgentDefinitionProvider> providers) {
        if (providers == null) {
            return;
        }
        for (AgentDefinitionProvider provider : providers) {
            for (AgentDefinition definition : provider.getDefinitions()) {
                if (definition != null && definition.getName() != null) {
                    definitions.put(definition.getName(), definition);
                }
            }
        }
    }

    @Override
    public AgentDefinition get(String name) {
        return definitions.get(name);
    }

    @Override
    public boolean contains(String name) {
        return definitions.containsKey(name);
    }

    @Override
    public void register(AgentDefinition definition) {
        if (definition != null && definition.getName() != null) {
            definitions.put(definition.getName(), definition);
        }
    }

    @Override
    public void remove(String name) {
        if (name != null) {
            definitions.remove(name);
        }
    }

    @Override
    public Collection<AgentDefinition> all() {
        return definitions.values();
    }
}
