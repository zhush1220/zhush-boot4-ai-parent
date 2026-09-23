package com.zhush.agentscope.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * 待人工审批事件（敏感工具挂起）。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class AgentApprovalRequiredEvent extends AgentEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 审批编号（Redis key 后缀，用于恢复执行）
     */
    private String approvalId;

    /**
     * 敏感工具名
     */
    private String toolName;

    /**
     * 工具入参（JSON 字符串，脱敏）
     */
    private String arguments;

    /**
     * AgentScope require-confirm 本轮标识（路线 B 续跑关联，路线 A 为空）
     */
    private String replyId;
}
