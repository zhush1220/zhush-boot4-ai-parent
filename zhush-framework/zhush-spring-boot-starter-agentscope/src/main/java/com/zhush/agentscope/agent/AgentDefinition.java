package com.zhush.agentscope.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 智能体定义（业务侧声明式定义）
 *
 * <p>一个 Agent 的完整描述：唯一名、系统提示词、模型、允许使用的工具、最大推理步数、技能清单。
 * 业务模块通过实现 {@link AgentDefinitionProvider} 声明，由 {@link AgentRegistry} 收集。
 *
 * @author zhush
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentDefinition implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 智能体唯一标识（如 ai-chat、ai-translator），全局唯一
     */
    private String name;

    /**
     * 功能说明（给前端展示）
     */
    private String description;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 模型名（如 qwen-max），缺省用默认模型
     */
    private String model;

    /**
     * 允许使用的工具名清单（裁剪 {@link com.zhush.agentscope.tool.ToolkitRegistry#buildToolkit}）
     */
    private List<String> toolNames;

    /**
     * 是否支持流式（SSE）
     */
    private boolean streaming;

    /**
     * 最大推理步数（成本/防失控上限）
     */
    private int maxSteps;

    /**
     * 技能清单（HarnessAgent 能力）
     */
    private List<String> skills;

    /**
     * 所属分组（common-公共 biz-业务，用于前端分组展示）
     */
    private String groupCode;
}
