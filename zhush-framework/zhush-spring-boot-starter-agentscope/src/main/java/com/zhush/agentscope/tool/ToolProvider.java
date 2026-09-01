package com.zhush.agentscope.tool;

import java.util.List;

/**
 * 手动注册工具扩展点：业务侧可不使用 {@link AgentTool} 注解，直接提供工具描述。
 *
 * @author zhush
 */
public interface ToolProvider {

    /**
     * 返回手动注册的工具（Bean 与方法的元数据）。
     *
     * @return 工具列表
     */
    List<RegisteredTool> getTools();
}
