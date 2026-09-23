package com.zhush.agentscope.safety;

/**
 * 审批落库 SPI：把敏感工具触发审批时生成的 {@link ApprovalRequest} 持久化到业务审批表
 * （{@code agent_approval}），供管理端审批列表可见、审批通过后据此续跑。
 *
 * <p>starter 不直连业务库，仅在 {@code approvalGate} 命中审批（{@link ToolApprovalHandler} 返回
 * PENDING）后回调本接口；由业务模块（zhush-module-ai）提供实现。未提供实现时续跑链路退化为
 * 「仅 Redis 状态、DB 不可见」，故 P0 要求业务侧必须注册本 Bean。
 *
 * @author zhush
 */
public interface ApprovalPersister {

    /**
     * 落库一条待审批记录（status=0）。实现须幂等容错：持久化异常不得阻断 SSE 主流程，
     * 内部自行 try-catch 并记录日志。
     *
     * @param request 审批请求（含 approvalId/sessionId/agentName/toolName/arguments/proposer/expireTime）
     */
    void persist(ApprovalRequest request);
}
