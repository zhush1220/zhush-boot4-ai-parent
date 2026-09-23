package com.zhush.ai.service;

import com.zhush.ai.context.AiSessionContextResolver;
import com.zhush.ai.entity.AgentApprovalEntity;
import com.zhush.ai.mapper.AgentApprovalMapper;
import com.zhush.agentscope.context.AgentContext;
import com.zhush.agentscope.safety.ToolApprovalHandler;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link AgentApprovalService} 审批续跑分支与幂等测试（S5 路线 B）：
 * <ul>
 *   <li>记录不存在 → 抛业务异常；</li>
 *   <li>非待审批状态 → 直接返回（幂等，不重复执行）；</li>
 *   <li>旧版（replyId 空）→ 走 Redis 快路 + DB 更新，不触发原生续跑；</li>
 *   <li>原生（replyId 非空）且 {@code resumed} CAS 0→1 抢到 → 事务外触发一次续跑；</li>
 *   <li>原生但 CAS 未抢到 → 幂等跳过，不续跑。</li>
 * </ul>
 *
 * <p>CAS 由 MyBatis-Flex {@code UpdateChain} 构建，其 SQL 语义非本用例目标；
 * 用 spy 打桩 {@code casFinish} 的布尔返回值，专注验证「分支选择 + 续跑触发/幂等跳过」这一自有逻辑。
 * 无活动事务同步，{@code runAfterCommit} 退化为同步执行，故可直接断言 {@code resumeNative} 调用。
 */
@ExtendWith(MockitoExtension.class)
class AgentApprovalServiceTest {

    @Mock
    private ToolApprovalHandler approvalHandler;
    @Mock
    private AgentApprovalMapper approvalMapper;
    @Mock
    private AiSessionContextResolver contextResolver;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private AgentChatService chatService;

    private AgentApprovalService service() {
        return new AgentApprovalService(approvalHandler, approvalMapper, contextResolver, objectMapper, chatService);
    }

    private AgentApprovalEntity entity(String approvalId, String replyId, Integer status) {
        AgentApprovalEntity e = new AgentApprovalEntity();
        e.setApprovalId(approvalId);
        e.setAgentName("ai-chat");
        e.setSessionId("sess-1");
        e.setReplyId(replyId);
        e.setStatus(status);
        return e;
    }

    private void stubApprover(long userId) {
        AgentContext ctx = mock(AgentContext.class);
        when(ctx.getUserId()).thenReturn(userId);
        when(contextResolver.resolve()).thenReturn(ctx);
    }

    @Test
    void approveThrowsWhenMissing() {
        when(approvalMapper.selectByApprovalId("missing")).thenReturn(null);

        assertThatThrownBy(() -> service().approve("missing", null))
                .isInstanceOf(com.zhush.common.exception.ServiceException.class);
    }

    @Test
    void approveAlreadyProcessedIsIdempotentNoop() {
        // status=1（已通过）：幂等直接返回，不触发续跑/Redis/DB
        when(approvalMapper.selectByApprovalId("a1")).thenReturn(entity("a1", "reply-9", 1));

        service().approve("a1", null);

        verifyNoInteractions(chatService);
        verifyNoInteractions(approvalHandler);
        verify(approvalMapper, never()).update(any());
    }

    @Test
    void approveLegacyUsesRedisFastPathWithoutResume() {
        // replyId 空 → 路线 A/旧版：更新 Redis 快路 + DB，不续跑
        when(approvalMapper.selectByApprovalId("a2")).thenReturn(entity("a2", "", 0));
        stubApprover(10L);

        service().approve("a2", null);

        verify(approvalHandler).approve(eq("a2"), any());
        verify(approvalMapper).update(any(AgentApprovalEntity.class));
        verifyNoInteractions(chatService);
    }

    @Test
    void rejectLegacyUsesRedisFastPathWithoutResume() {
        when(approvalMapper.selectByApprovalId("a3")).thenReturn(entity("a3", null, 0));
        stubApprover(11L);

        service().reject("a3", "不合适");

        verify(approvalHandler).reject(eq("a3"), eq("不合适"));
        verify(approvalMapper).update(any(AgentApprovalEntity.class));
        verifyNoInteractions(chatService);
    }

    @Test
    void approveNativeCasWinsTriggersResumeOnce() {
        when(approvalMapper.selectByApprovalId("n1")).thenReturn(entity("n1", "reply-9", 0));
        stubApprover(20L);
        AgentApprovalService svc = spy(service());
        // 抢到 CAS（resumed 0→1）：status 写为已通过(1)
        doReturn(true).when(svc).casFinish(anyString(), any(), any(), anyInt(), any(), any());

        svc.approve("n1", null);

        verify(svc).casFinish(eq("n1"), eq(20L), any(), eq(1), any(), any());
        // 以首轮提交语义续跑（confirmed=true），且不走旧版 Redis 快路
        verify(chatService).resumeNative("ai-chat", "sess-1", "reply-9", true, null);
        verify(approvalHandler, never()).approve(any(), any());
    }

    @Test
    void approveNativeCasLosesSkipsResume() {
        when(approvalMapper.selectByApprovalId("n2")).thenReturn(entity("n2", "reply-9", 0));
        stubApprover(21L);
        AgentApprovalService svc = spy(service());
        // CAS 未抢到（resumed 已被并发置 1）
        doReturn(false).when(svc).casFinish(anyString(), any(), any(), anyInt(), any(), any());

        svc.approve("n2", null);

        verifyNoInteractions(chatService);
        verify(approvalHandler, never()).approve(any(), any());
    }

    @Test
    void rejectNativeCasWinsTriggersRejectResume() {
        when(approvalMapper.selectByApprovalId("n3")).thenReturn(entity("n3", "reply-9", 0));
        stubApprover(22L);
        AgentApprovalService svc = spy(service());
        doReturn(true).when(svc).casFinish(anyString(), any(), any(), anyInt(), any(), any());

        svc.reject("n3", "违规");

        // status 写为已驳回(2)
        verify(svc).casFinish(eq("n3"), eq(22L), any(), eq(2), any(), eq("违规"));
        // confirmed=false 配合 stopOnReject 让框架产出解释性收尾
        verify(chatService).resumeNative("ai-chat", "sess-1", "reply-9", false, null);
        verify(approvalHandler, never()).reject(any(), any());
    }
}
