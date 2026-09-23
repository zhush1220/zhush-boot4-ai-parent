package com.zhush.agentscope.safety;

import java.util.Map;

/**
 * 敏感工具审批处理器 SPI。
 *
 * <p>默认实现将待审批任务写入 Redis（key: agentscope:approval:{sessionId}:{tool}），
 * 并提供 REST 接口供管理端审批 / 驳回 / 改参；审批后精确恢复执行。
 *
 * @author zhush
 */
public interface ToolApprovalHandler {

    /**
     * 提交审批（挂起执行）。
     *
     * @param request 审批请求
     * @return 审批状态
     */
    ApprovalStatus submit(ApprovalRequest request);

    /**
     * 通过（可改参）。
     *
     * @param approvalId   审批编号
     * @param modifiedArgs 修改后的入参（为 null 表示不修改）
     */
    void approve(String approvalId, Map<String, Object> modifiedArgs);

    /**
     * 驳回。
     *
     * @param approvalId   审批编号
     * @param reason 驳回原因
     */
    void reject(String approvalId, String reason);

    /**
     * 审批状态是否可持久化（默认 {@code true}）。
     *
     * <p>无 Redis 降级实现（{@link NullToolApprovalHandler}）返回 {@code false}：审批无法落库、
     * 续跑无从谈起，据此触发降级护栏（放行留痕或按开关 fail-fast）。
     */
    default boolean persistent() {
        return true;
    }
}
