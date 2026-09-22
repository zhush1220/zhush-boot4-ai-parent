package com.zhush.ai.listener;

import com.mybatisflex.core.update.UpdateChain;
import com.zhush.ai.entity.AgentTokenUsageEntity;
import com.zhush.ai.mapper.AgentTokenUsageMapper;
import com.zhush.agentscope.config.AgentScopeProperties;
import com.zhush.agentscope.event.AgentEvent;
import com.zhush.agentscope.event.AgentEventListener;
import com.zhush.agentscope.event.AgentFinishedEvent;
import com.zhush.agentscope.model.AgentModelResolver;
import com.zhush.agentscope.model.ModelPricing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 用量事件监听器：把会话结束事件（{@link AgentFinishedEvent}）的 Token 计量累加到
 * {@code agent_token_usage} 日聚合表（日期 + 租户 + 智能体维度）。
 *
 * <p>事件由 {@code AgentEventPublisher} 异步分发，本监听器在独立线程执行，
 * 因此累加必须走数据库原子自增（{@code setRaw}）而非「查-改-写」，避免并发丢量。
 *
 * <p>成本按方案①（模型单价配置于 {@code ai_model}）折算：单价随动态模型池缓存在
 * {@link AgentModelResolver}，此处不查库；未配单价或 {@code cost-enabled=false} 时成本记 0，
 * 即「只计量不计费」，不阻断主流程。
 *
 * <p>聚合行只增不删，禁止逻辑删除（{@code is_delete} 参与唯一键，删过的行会与新行撞键）。
 *
 * @author zhush
 */
@Component
public class AgentUsageEventListener implements AgentEventListener {

    private static final Logger log = LoggerFactory.getLogger(AgentUsageEventListener.class);

    private final AgentTokenUsageMapper tokenUsageMapper;
    private final AgentModelResolver modelResolver;
    private final AgentScopeProperties properties;

    public AgentUsageEventListener(AgentTokenUsageMapper tokenUsageMapper,
                                   AgentModelResolver modelResolver,
                                   AgentScopeProperties properties) {
        this.tokenUsageMapper = tokenUsageMapper;
        this.modelResolver = modelResolver;
        this.properties = properties;
    }

    @Override
    public void onEvent(AgentEvent event) {
        if (!(event instanceof AgentFinishedEvent finished)) {
            return;
        }
        if (properties.getUsage() == null || !properties.getUsage().isEnabled()) {
            return;
        }
        int inputTokens = Math.max(finished.getInputTokens(), 0);
        int outputTokens = Math.max(finished.getOutputTokens(), 0);
        int totalTokens = finished.getTotalTokens() > 0
                ? finished.getTotalTokens() : inputTokens + outputTokens;
        LocalDate usageDate = resolveDate(finished.getTime());
        Long tenantId = parseLong(finished.getTenantId());
        String agentName = finished.getAgentName() == null ? "" : finished.getAgentName();
        BigDecimal cost = resolveCost(finished, inputTokens, outputTokens);

        try {
            // 先原子累加，命中已有聚合行即结束；影响 0 行说明当天首条，转插入
            if (accumulate(usageDate, tenantId, agentName, inputTokens, outputTokens, totalTokens, cost)) {
                return;
            }
            insertFirst(usageDate, tenantId, agentName, inputTokens, outputTokens, totalTokens, cost);
        } catch (Exception e) {
            log.warn("AI 用量日聚合落库失败, agentName={}, sessionId={}, error={}",
                    agentName, finished.getSessionId(), e.getMessage());
        }
    }

    /**
     * 已存在则数据库侧原子累加。
     *
     * @return 是否更新到已有聚合行
     */
    private boolean accumulate(LocalDate usageDate, Long tenantId, String agentName,
                               int inputTokens, int outputTokens, int totalTokens, BigDecimal cost) {
        return UpdateChain.create(tokenUsageMapper)
                .setRaw(AgentTokenUsageEntity::getRequestCount, "request_count + 1")
                .setRaw(AgentTokenUsageEntity::getInputTokens, "input_tokens + " + inputTokens)
                .setRaw(AgentTokenUsageEntity::getOutputTokens, "output_tokens + " + outputTokens)
                .setRaw(AgentTokenUsageEntity::getTotalTokens, "total_tokens + " + totalTokens)
                .setRaw(AgentTokenUsageEntity::getCost, "cost + " + cost.toPlainString())
                .where(AgentTokenUsageEntity::getUsageDate).eq(usageDate)
                .and(AgentTokenUsageEntity::getTenantId).eq(tenantId)
                .and(AgentTokenUsageEntity::getAgentName).eq(agentName)
                .update();
    }

    /**
     * 当天首条：插入聚合行；并发插入撞唯一键时回退为累加。
     */
    private void insertFirst(LocalDate usageDate, Long tenantId, String agentName,
                             int inputTokens, int outputTokens, int totalTokens, BigDecimal cost) {
        AgentTokenUsageEntity entity = new AgentTokenUsageEntity();
        entity.setUsageDate(usageDate);
        entity.setTenantId(tenantId);
        entity.setAgentName(agentName);
        entity.setRequestCount(1);
        entity.setInputTokens((long) inputTokens);
        entity.setOutputTokens((long) outputTokens);
        entity.setTotalTokens((long) totalTokens);
        entity.setCost(cost);
        try {
            tokenUsageMapper.insert(entity);
        } catch (Exception e) {
            // 多实例/多线程同时插第一条：唯一键冲突说明行已被对方建立，转为累加即可
            accumulate(usageDate, tenantId, agentName, inputTokens, outputTokens, totalTokens, cost);
        }
    }

    private BigDecimal resolveCost(AgentFinishedEvent finished, int inputTokens, int outputTokens) {
        if (!properties.getUsage().isCostEnabled()) {
            return zero();
        }
        ModelPricing pricing = modelResolver.resolvePricing(finished.getModel());
        if (pricing == null || !pricing.hasPrice()) {
            return zero();
        }
        return pricing.calculate(inputTokens, outputTokens);
    }

    private BigDecimal zero() {
        return BigDecimal.ZERO.setScale(ModelPricing.COST_SCALE, RoundingMode.HALF_UP);
    }

    private LocalDate resolveDate(Instant time) {
        return time == null ? LocalDate.now()
                : LocalDate.ofInstant(time, ZoneId.systemDefault());
    }

    private Long parseLong(String value) {
        try {
            return value == null ? 0L : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
