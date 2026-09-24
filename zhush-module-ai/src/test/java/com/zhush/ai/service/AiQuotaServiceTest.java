package com.zhush.ai.service;

import com.zhush.ai.entity.AiQuotaRuleEntity;
import com.zhush.ai.entity.AiUsageRecordEntity;
import com.zhush.ai.enums.AiTaskType;
import com.zhush.ai.mapper.AiQuotaRuleMapper;
import com.zhush.ai.mapper.AiUsageRecordMapper;
import com.zhush.agentscope.config.AgentScopeProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link AiQuotaService#assertAllowed} 判定契约测试（方案 §4.1）：
 * 总开关放行、管理员豁免、无规则/0 上限不限、日用量与月用量边界（used &gt;= limit 即拒）。
 *
 * <p>只测判定侧；计数侧涉及 {@code UpdateChain} 原子自增（DB 语义），不在纯单测范围。
 */
@ExtendWith(MockitoExtension.class)
class AiQuotaServiceTest {

    @Mock
    private AiQuotaRuleMapper quotaRuleMapper;
    @Mock
    private AiUsageRecordMapper usageRecordMapper;

    private AiQuotaService service(boolean enabled) {
        AgentScopeProperties props = new AgentScopeProperties();
        props.getQuota().setEnabled(enabled);
        return new AiQuotaService(quotaRuleMapper, usageRecordMapper, props);
    }

    private AiQuotaRuleEntity rule(int dailyLimit, int monthlyLimit) {
        AiQuotaRuleEntity entity = new AiQuotaRuleEntity();
        entity.setUserType("member");
        entity.setTaskType(AiTaskType.CHAT.getCode());
        entity.setDailyLimit(dailyLimit);
        entity.setMonthlyLimit(monthlyLimit);
        entity.setStatus(AiQuotaRuleEntity.STATUS_ENABLED);
        return entity;
    }

    private AiUsageRecordEntity usage(int requestCount) {
        AiUsageRecordEntity entity = new AiUsageRecordEntity();
        entity.setRequestCount(requestCount);
        return entity;
    }

    @Test
    void disabledQuotaShouldPassWithoutAnyQuery() {
        service(false).assertAllowed(7L, "member", AiTaskType.CHAT);

        verifyNoInteractions(quotaRuleMapper, usageRecordMapper);
    }

    @Test
    void adminAndAnonymousShouldBeExempt() {
        service(true).assertAllowed(7L, "admin", AiTaskType.IMAGE);
        service(true).assertAllowed(0L, "member", AiTaskType.IMAGE);

        verifyNoInteractions(quotaRuleMapper, usageRecordMapper);
    }

    @Test
    void missingRuleOrZeroLimitsShouldMeanUnlimited() {
        when(quotaRuleMapper.selectOneByQuery(any())).thenReturn(null);
        assertThatCode(() -> service(true).assertAllowed(7L, "member", AiTaskType.CHAT))
                .doesNotThrowAnyException();

        when(quotaRuleMapper.selectOneByQuery(any())).thenReturn(rule(0, 0));
        assertThatCode(() -> service(true).assertAllowed(7L, "member", AiTaskType.CHAT))
                .doesNotThrowAnyException();
        // 0=不限：未触达用量查询
        verifyNoInteractions(usageRecordMapper);
    }

    @Test
    void dailyUsageAtLimitShouldReject() {
        when(quotaRuleMapper.selectOneByQuery(any())).thenReturn(rule(5, 0));
        when(usageRecordMapper.selectOneByQuery(any())).thenReturn(usage(5));

        assertThatThrownBy(() -> service(true).assertAllowed(7L, "member", AiTaskType.CHAT))
                .isInstanceOf(com.zhush.common.exception.ServiceException.class)
                .hasMessageContaining("今日调用次数已用尽");
    }

    @Test
    void monthlyUsageAtLimitShouldRejectEvenIfDailyHasHeadroom() {
        when(quotaRuleMapper.selectOneByQuery(any())).thenReturn(rule(10, 6));
        when(usageRecordMapper.selectOneByQuery(any())).thenReturn(usage(3));
        when(usageRecordMapper.selectListByQuery(any()))
                .thenReturn(java.util.List.of(usage(2), usage(4)));

        assertThatThrownBy(() -> service(true).assertAllowed(7L, "member", AiTaskType.CHAT))
                .isInstanceOf(com.zhush.common.exception.ServiceException.class)
                .hasMessageContaining("本月调用次数已用尽");
    }

    @Test
    void usageBelowBothLimitsShouldPass() {
        when(quotaRuleMapper.selectOneByQuery(any())).thenReturn(rule(5, 100));
        when(usageRecordMapper.selectOneByQuery(any())).thenReturn(usage(4));
        when(usageRecordMapper.selectListByQuery(any())).thenReturn(java.util.List.of(usage(40)));

        assertThatCode(() -> service(true).assertAllowed(7L, "member", AiTaskType.CHAT))
                .doesNotThrowAnyException();
    }
}
