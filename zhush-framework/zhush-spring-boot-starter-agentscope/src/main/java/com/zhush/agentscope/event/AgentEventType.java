package com.zhush.agentscope.event;

/**
 * 会话生命周期事件类型（与 AgentScope 事件系统桥接）。
 *
 * @author zhush
 */
public enum AgentEventType {

    /**
     * 会话开始（进入推理）
     */
    STARTED,

    /**
     * 文本增量（流式推送）
     */
    TEXT_DELTA,

    /**
     * 工具调用（含参数/结果/是否通过审批）
     */
    TOOL_CALLED,

    /**
     * 会话结束（含耗时/Token 消耗）
     */
    FINISHED,

    /**
     * 异常（错误信息）
     */
    ERROR,

    /**
     * 待人工审批（敏感工具挂起）
     */
    APPROVAL_REQUIRED
}
