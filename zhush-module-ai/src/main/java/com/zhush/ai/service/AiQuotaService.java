package com.zhush.ai.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.zhush.ai.entity.AiQuotaRuleEntity;
import com.zhush.ai.entity.AiUsageRecordEntity;
import com.zhush.ai.enums.AiTaskType;
import com.zhush.ai.mapper.AiQuotaRuleMapper;
import com.zhush.ai.mapper.AiUsageRecordMapper;
import com.zhush.agentscope.config.AgentScopeProperties;
import com.zhush.common.exception.ServiceExceptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.zhush.ai.enums.AiErrorCode.AI_QUOTA_DAILY_EXCEED;
import static com.zhush.ai.enums.AiErrorCode.AI_QUOTA_MONTHLY_EXCEED;

/**
 * AI 配额服务：按「用户 + 用户类型 + 任务类型」判定日/月调用次数是否超限，并在成功后原子计数。
 *
 * <p>判定口径：以 {@code ai_usage_record}（按人按日按任务聚合）的 {@code request_count} 为准；
 * 规则表 {@code ai_quota_rule} 只存阈值不存用量。{@code daily_limit/monthly_limit} 为 0 表示该维度不限。
 *
 * <p>豁免：{@code user_type=admin} 直接放行（管理端不限量）；配额总开关 {@code zhush.agentscope.quota.enabled=false}
 * 时既不校验也不计数（用于灰度与故障隔离）。
 *
 * <p>计数走 DB 原子自增（照 {@code AgentUsageEventListener}「先 UPDATE，0 行则 INSERT，撞唯一键回退累加」写法），
 * 保证多实例并发不丢量；<b>调用方仅在「请求成功发起」后 record，失败不计数</b>（见方案风险 R3：
 * 已消耗供应商额度的转存失败由生成服务显式计入 image_count/video_count）。
 *
 * @author zhush
 */
@Service
public class AiQuotaService {

    private static final Logger log = LoggerFactory.getLogger(AiQuotaService.class);

    private static final String USER_TYPE_ADMIN = "admin";

    private final AiQuotaRuleMapper quotaRuleMapper;
    private final AiUsageRecordMapper usageRecordMapper;
    private final AgentScopeProperties properties;

    public AiQuotaService(AiQuotaRuleMapper quotaRuleMapper,
                         AiUsageRecordMapper usageRecordMapper,
                         AgentScopeProperties properties) {
        this.quotaRuleMapper = quotaRuleMapper;
        this.usageRecordMapper = usageRecordMapper;
        this.properties = properties;
    }

    /**
     * 校验当前用户对该任务类型是否还有配额，超限抛业务异常。
     */
    public void assertAllowed(Long userId, String userType, AiTaskType taskType) {
        AgentScopeProperties.Quota quota = properties.getQuota();
        if (quota == null || !quota.isEnabled()) {
            return;
        }
        // 管理员豁免 + 无身份（未登录兜底 userId=0 视为系统内部调用）直接放行
        if (USER_TYPE_ADMIN.equals(userType) || userId == null || userId <= 0L) {
            return;
        }
        AiTaskType type = taskType == null ? AiTaskType.CHAT : taskType;
        AiQuotaRuleEntity rule = loadRule(userType, type);
        if (rule == null) {
            return;
        }
        int dailyLimit = rule.getDailyLimit() == null ? 0 : rule.getDailyLimit();
        int monthlyLimit = rule.getMonthlyLimit() == null ? 0 : rule.getMonthlyLimit();
        if (dailyLimit <= 0 && monthlyLimit <= 0) {
            return;
        }
        LocalDate today = LocalDate.now();
        if (dailyLimit > 0) {
            int used = dailyUsage(userId, userType, type, today);
            if (used >= dailyLimit) {
                throw ServiceExceptionUtil.exception(AI_QUOTA_DAILY_EXCEED);
            }
        }
        if (monthlyLimit > 0) {
            int used = monthlyUsage(userId, userType, type, today);
            if (used >= monthlyLimit) {
                throw ServiceExceptionUtil.exception(AI_QUOTA_MONTHLY_EXCEED);
            }
        }
    }

    /**
     * 记一次文本类调用（对话/导出/解析）：request_count + 1，并累加 Token 与成本。
     */
    public void record(Long userId, String userType, AiTaskType taskType,
                       int inputTokens, int outputTokens, BigDecimal cost) {
        accumulate(userId, userType, taskType, 1, 0, 0, inputTokens, outputTokens, cost);
    }

    /**
     * 记一次文本类调用（不含成本）：成本归属由 {@code agent_token_usage} 侧统一核算，
     * 本表 request_count/tokens 用于配额判定与「按人」可追溯。
     */
    public void record(Long userId, String userType, AiTaskType taskType,
                       int inputTokens, int outputTokens) {
        record(userId, userType, taskType, inputTokens, outputTokens, BigDecimal.ZERO);
    }

    /**
     * 记一次生成类调用：request_count 与对应 image_count/video_count 各 + 1（成本归因，见 R3）。
     */
    public void recordGeneration(Long userId, String userType, AiTaskType taskType) {
        if (taskType == AiTaskType.IMAGE) {
            accumulate(userId, userType, taskType, 1, 1, 0, 0, 0, BigDecimal.ZERO);
        } else if (taskType == AiTaskType.VIDEO) {
            accumulate(userId, userType, taskType, 1, 0, 1, 0, 0, BigDecimal.ZERO);
        } else {
            accumulate(userId, userType, taskType, 1, 0, 0, 0, 0, BigDecimal.ZERO);
        }
    }

    // ================ 内部 ================

    private AiQuotaRuleEntity loadRule(String userType, AiTaskType taskType) {
        return quotaRuleMapper.selectOneByQuery(QueryWrapper.create()
                .where(AiQuotaRuleEntity::getUserType).eq(userType)
                .and(AiQuotaRuleEntity::getTaskType).eq(taskType.getCode())
                .and(AiQuotaRuleEntity::getStatus).eq(AiQuotaRuleEntity.STATUS_ENABLED));
    }

    private int dailyUsage(Long userId, String userType, AiTaskType taskType, LocalDate date) {
        AiUsageRecordEntity row = usageRecordMapper.selectOneByQuery(QueryWrapper.create()
                .where(AiUsageRecordEntity::getUserId).eq(userId)
                .and(AiUsageRecordEntity::getUserType).eq(userType)
                .and(AiUsageRecordEntity::getTaskType).eq(taskType.getCode())
                .and(AiUsageRecordEntity::getUsageDate).eq(date));
        return row == null || row.getRequestCount() == null ? 0 : row.getRequestCount();
    }

    private int monthlyUsage(Long userId, String userType, AiTaskType taskType, LocalDate today) {
        LocalDate first = today.withDayOfMonth(1);
        List<AiUsageRecordEntity> rows = usageRecordMapper.selectListByQuery(QueryWrapper.create()
                .where(AiUsageRecordEntity::getUserId).eq(userId)
                .and(AiUsageRecordEntity::getUserType).eq(userType)
                .and(AiUsageRecordEntity::getTaskType).eq(taskType.getCode())
                .and(AiUsageRecordEntity::getUsageDate).between(first, today));
        int sum = 0;
        for (AiUsageRecordEntity row : rows) {
            sum += row.getRequestCount() == null ? 0 : row.getRequestCount();
        }
        return sum;
    }

    private void accumulate(Long userId, String userType, AiTaskType taskType,
                            int requestDelta, int imageDelta, int videoDelta,
                            int inputTokens, int outputTokens, BigDecimal cost) {
        AgentScopeProperties.Quota quota = properties.getQuota();
        if (quota == null || !quota.isEnabled()) {
            return;
        }
        if (userId == null || userId <= 0L) {
            return;
        }
        AiTaskType type = taskType == null ? AiTaskType.CHAT : taskType;
        LocalDate today = LocalDate.now();
        int totalTokens = Math.max(inputTokens, 0) + Math.max(outputTokens, 0);
        BigDecimal safeCost = cost == null ? BigDecimal.ZERO : cost;
        try {
            if (doAccumulate(userId, userType, type, today, requestDelta, imageDelta, videoDelta,
                    inputTokens, outputTokens, totalTokens, safeCost)) {
                return;
            }
            insertFirst(userId, userType, type, today, requestDelta, imageDelta, videoDelta,
                    inputTokens, outputTokens, totalTokens, safeCost);
        } catch (Exception e) {
            // 配额计数失败不阻断主流程（用量可事后对账），仅告警
            log.warn("AI 配额计数失败, userId={}, userType={}, taskType={}, error={}",
                    userId, userType, type.getCode(), e.getMessage());
        }
    }

    private boolean doAccumulate(Long userId, String userType, AiTaskType type, LocalDate date,
                                 int requestDelta, int imageDelta, int videoDelta,
                                 int inputTokens, int outputTokens, int totalTokens, BigDecimal cost) {
        UpdateChain<AiUsageRecordEntity> chain = UpdateChain.create(usageRecordMapper)
                .setRaw(AiUsageRecordEntity::getRequestCount, "request_count + " + requestDelta)
                .setRaw(AiUsageRecordEntity::getInputTokens, "input_tokens + " + inputTokens)
                .setRaw(AiUsageRecordEntity::getOutputTokens, "output_tokens + " + outputTokens)
                .setRaw(AiUsageRecordEntity::getTotalTokens, "total_tokens + " + totalTokens)
                .setRaw(AiUsageRecordEntity::getCost, "cost + " + cost.toPlainString());
        if (imageDelta > 0) {
            chain.setRaw(AiUsageRecordEntity::getImageCount, "image_count + " + imageDelta);
        }
        if (videoDelta > 0) {
            chain.setRaw(AiUsageRecordEntity::getVideoCount, "video_count + " + videoDelta);
        }
        return chain
                .where(AiUsageRecordEntity::getUserId).eq(userId)
                .and(AiUsageRecordEntity::getUserType).eq(userType)
                .and(AiUsageRecordEntity::getTaskType).eq(type.getCode())
                .and(AiUsageRecordEntity::getUsageDate).eq(date)
                .update();
    }

    private void insertFirst(Long userId, String userType, AiTaskType type, LocalDate date,
                             int requestDelta, int imageDelta, int videoDelta,
                             int inputTokens, int outputTokens, int totalTokens, BigDecimal cost) {
        AiUsageRecordEntity entity = new AiUsageRecordEntity();
        entity.setUsageDate(date);
        entity.setUserId(userId);
        entity.setUserType(userType);
        entity.setTaskType(type.getCode());
        entity.setRequestCount(requestDelta);
        entity.setImageCount(imageDelta);
        entity.setVideoCount(videoDelta);
        entity.setInputTokens((long) Math.max(inputTokens, 0));
        entity.setOutputTokens((long) Math.max(outputTokens, 0));
        entity.setTotalTokens((long) totalTokens);
        entity.setCost(cost);
        try {
            usageRecordMapper.insert(entity);
        } catch (Exception e) {
            // 并发插首行撞唯一键：转累加
            doAccumulate(userId, userType, type, date, requestDelta, imageDelta, videoDelta,
                    inputTokens, outputTokens, totalTokens, cost);
        }
    }
}
