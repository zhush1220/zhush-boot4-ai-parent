package com.zhush.agentscope.safety;

/**
 * 审批状态。
 *
 * @author zhush
 */
public enum ApprovalStatus {

    /**
     * 待审批（挂起执行）
     */
    PENDING,

    /**
     * 已通过（可恢复执行）
     */
    APPROVED,

    /**
     * 已驳回（终止执行）
     */
    REJECTED,

    /**
     * 已过期（超时未审）
     */
    EXPIRED
}
