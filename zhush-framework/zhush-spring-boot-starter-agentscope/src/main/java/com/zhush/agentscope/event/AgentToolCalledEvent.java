package com.zhush.agentscope.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * 工具调用事件（含参数 / 结果 / 是否需审批）。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class AgentToolCalledEvent extends AgentEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 工具调用 ID（AgentScope toolCallId，供与 {@link AgentToolResultEvent} 配对）
     */
    private String id;

    /**
     * 工具名
     */
    private String toolName;

    /**
     * 工具入参（JSON 字符串，可能脱敏）
     */
    private String arguments;

    /**
     * 调用结果（JSON 字符串，超长截断）
     */
    private String result;

    /**
     * 是否需人工审批
     */
    private boolean requiresApproval;

    /**
     * 是否已通过审批（false 表示挂起等待）
     */
    private boolean approved;
}
