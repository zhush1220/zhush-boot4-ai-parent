package com.zhush.agentscope.agent;

import java.util.List;

/**
 * 业务侧扩展点：向注册中心提供 Agent 定义。
 *
 * <p>任何业务模块（union / member / ...）只需实现本接口并注册为 Spring Bean，
 * 框架自动收集其 {@link AgentDefinition} 到 {@link AgentRegistry}，实现"新增业务系统零改动框架"。
 *
 * @author zhush
 */
public interface AgentDefinitionProvider {

    /**
     * 返回该业务声明的全部 Agent 定义。
     *
     * @return 智能体定义列表（可为空）
     */
    List<AgentDefinition> getDefinitions();
}
