package com.zhush.agentscope.safety;

import java.util.Map;

/**
 * 无 Redis 时的审批降级实现：直接放行（非敏感或演示场景），避免敏感工具永久挂起。
 *
 * <p>生产使用 {@link DefaultToolApprovalHandler}（写入 Redis 人工审批）。
 *
 * @author zhush
 */
public class NullToolApprovalHandler implements ToolApprovalHandler {

    @Override
    public ApprovalStatus submit(ApprovalRequest request) {
        return ApprovalStatus.APPROVED;
    }

    @Override
    public void approve(String approvalId, Map<String, Object> modifiedArgs) {
        // 无需处理
    }

    @Override
    public void reject(String approvalId, String reason) {
        // 无需处理
    }

    @Override
    public boolean persistent() {
        // 无 Redis：审批状态不可持久化，触发降级护栏
        return false;
    }
}
