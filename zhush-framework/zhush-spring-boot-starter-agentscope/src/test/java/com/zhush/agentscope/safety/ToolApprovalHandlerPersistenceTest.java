package com.zhush.agentscope.safety;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 审批处理器持久化能力测试（S3 无 Redis 降级护栏的判定依据）：
 * {@link ToolApprovalHandler#persistent()} 默认 true，{@link NullToolApprovalHandler} 覆写为 false，
 * 据此在 {@code approvalGate} 触发 fail-fast 或降级放行留痕。
 */
class ToolApprovalHandlerPersistenceTest {

    @Test
    void defaultHandlerReportsPersistent() {
        // 接口默认实现：可持久化（生产 Redis 实现即走此默认）
        ToolApprovalHandler handler = new ToolApprovalHandler() {
            @Override
            public ApprovalStatus submit(ApprovalRequest request) {
                return ApprovalStatus.PENDING;
            }

            @Override
            public void approve(String approvalId, Map<String, Object> modifiedArgs) {
            }

            @Override
            public void reject(String approvalId, String reason) {
            }
        };

        assertThat(handler.persistent()).isTrue();
    }

    @Test
    void nullHandlerReportsNonPersistentAndAutoApproves() {
        NullToolApprovalHandler handler = new NullToolApprovalHandler();

        // 无 Redis：状态不可持久化，且 submit 直接返回 APPROVED（放行语义）
        assertThat(handler.persistent()).isFalse();
        assertThat(handler.submit(ApprovalRequest.builder().approvalId("a1").build()))
                .isEqualTo(ApprovalStatus.APPROVED);
    }
}
