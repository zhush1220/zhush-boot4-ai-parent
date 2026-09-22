package com.zhush.ai.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.ai.entity.AgentTokenUsageEntity;
import com.zhush.ai.mapper.AgentTokenUsageMapper;
import com.zhush.ai.query.AgentUsageQuery;
import com.zhush.ai.vo.AgentUsageSummaryVO;
import com.zhush.ai.vo.AgentUsageTrendVO;
import com.zhush.ai.vo.AgentUsageVO;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * AI 用量统计服务（只读报表）。
 *
 * <p>数据来自 {@code agent_token_usage} 日聚合表，由 {@code AgentUsageEventListener} 异步累加。
 *
 * @author zhush
 */
@Service
public class AgentUsageService {

    /**
     * 成本展示口径，与聚合列 DECIMAL(12,4) 一致
     */
    private static final int COST_SCALE = 4;

    /**
     * 趋势图日期补零上限（超出则只返回实际有用量的日期，避免构造超长横轴）
     */
    private static final int MAX_FILL_DAYS = 366;

    private final AgentTokenUsageMapper tokenUsageMapper;

    public AgentUsageService(AgentTokenUsageMapper tokenUsageMapper) {
        this.tokenUsageMapper = tokenUsageMapper;
    }

    /**
     * 用量明细分页（日期倒序，同日按 Token 量倒序）。
     */
    public PageResult<AgentUsageVO> page(AgentUsageQuery query) {
        QueryWrapperX queryWrapper = buildQueryWrapper(query);
        queryWrapper.orderBy("usage_date", false);
        queryWrapper.orderBy("total_tokens", false);
        Page<AgentTokenUsageEntity> page = tokenUsageMapper.selectPageX(query, queryWrapper);
        List<AgentUsageVO> list = new ArrayList<>(page.getRecords().size());
        for (AgentTokenUsageEntity entity : page.getRecords()) {
            list.add(toVO(entity));
        }
        return PageResult.of(list, page.getTotalRow());
    }

    /**
     * 当前筛选条件下的合计。
     *
     * <p>聚合表一行代表「一天 × 一租户 × 一智能体」，量级有限，故取回后在内存累加，
     * 不为该表引入自定义 SQL。
     */
    public AgentUsageSummaryVO summary(AgentUsageQuery query) {
        List<AgentTokenUsageEntity> list = loadUsageList(query);
        long requestCount = 0L;
        long inputTokens = 0L;
        long outputTokens = 0L;
        long totalTokens = 0L;
        BigDecimal cost = BigDecimal.ZERO;
        Set<LocalDate> usageDates = new HashSet<>();
        for (AgentTokenUsageEntity entity : list) {
            requestCount += nullToZero(entity.getRequestCount());
            inputTokens += nullToZero(entity.getInputTokens());
            outputTokens += nullToZero(entity.getOutputTokens());
            totalTokens += nullToZero(entity.getTotalTokens());
            if (entity.getCost() != null) {
                cost = cost.add(entity.getCost());
            }
            if (entity.getUsageDate() != null) {
                usageDates.add(entity.getUsageDate());
            }
        }

        AgentUsageSummaryVO summary = new AgentUsageSummaryVO();
        summary.setRequestCount(requestCount);
        summary.setInputTokens(inputTokens);
        summary.setOutputTokens(outputTokens);
        summary.setTotalTokens(totalTokens);
        summary.setCost(cost.setScale(COST_SCALE, RoundingMode.HALF_UP));
        summary.setAvgTokensPerRequest(requestCount == 0L ? 0L : Math.round((double) totalTokens / requestCount));
        summary.setUsageDays(usageDates.size());
        return summary;
    }

    /**
     * 每日趋势（跨租户/跨智能体再按日期聚合，日期升序）。
     *
     * <p>同时指定起止日期且区间不超 {@link #MAX_FILL_DAYS} 时，无量的日期补 0 点，
     * 避免图表横轴跳过空白天导致趋势失真。
     */
    public List<AgentUsageTrendVO> trend(AgentUsageQuery query) {
        Map<LocalDate, AgentUsageTrendVO> grouped = new TreeMap<>();
        for (AgentTokenUsageEntity entity : loadUsageList(query)) {
            LocalDate usageDate = entity.getUsageDate();
            if (usageDate == null) {
                continue;
            }
            AgentUsageTrendVO item = grouped.computeIfAbsent(usageDate, this::emptyTrend);
            item.setRequestCount(item.getRequestCount() + nullToZero(entity.getRequestCount()));
            item.setInputTokens(item.getInputTokens() + nullToZero(entity.getInputTokens()));
            item.setOutputTokens(item.getOutputTokens() + nullToZero(entity.getOutputTokens()));
            item.setTotalTokens(item.getTotalTokens() + nullToZero(entity.getTotalTokens()));
            if (entity.getCost() != null) {
                item.setCost(item.getCost().add(entity.getCost()));
            }
        }
        fillMissingDays(grouped, query);

        List<AgentUsageTrendVO> list = new ArrayList<>(grouped.size());
        for (AgentUsageTrendVO item : grouped.values()) {
            item.setCost(item.getCost().setScale(COST_SCALE, RoundingMode.HALF_UP));
            list.add(item);
        }
        return list;
    }

    /**
     * 按筛选条件取聚合行（合计与趋势共用，口径一致）。
     */
    private List<AgentTokenUsageEntity> loadUsageList(AgentUsageQuery query) {
        return tokenUsageMapper.selectListByQuery(buildQueryWrapper(query));
    }

    private void fillMissingDays(Map<LocalDate, AgentUsageTrendVO> grouped, AgentUsageQuery query) {
        LocalDate startDate = query.getStartDate();
        LocalDate endDate = query.getEndDate();
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return;
        }
        if (startDate.plusDays(MAX_FILL_DAYS).isBefore(endDate)) {
            return;
        }
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            grouped.putIfAbsent(date, emptyTrend(date));
        }
    }

    private AgentUsageTrendVO emptyTrend(LocalDate usageDate) {
        AgentUsageTrendVO vo = new AgentUsageTrendVO();
        vo.setUsageDate(usageDate);
        vo.setRequestCount(0L);
        vo.setInputTokens(0L);
        vo.setOutputTokens(0L);
        vo.setTotalTokens(0L);
        vo.setCost(BigDecimal.ZERO);
        return vo;
    }

    private QueryWrapperX buildQueryWrapper(AgentUsageQuery query) {
        return QueryWrapperX.create()
                .ge(AgentTokenUsageEntity::getUsageDate, query.getStartDate())
                .le(AgentTokenUsageEntity::getUsageDate, query.getEndDate())
                .like(AgentTokenUsageEntity::getAgentName, query.getAgentName())
                .eq(AgentTokenUsageEntity::getTenantId, query.getTenantId());
    }

    private AgentUsageVO toVO(AgentTokenUsageEntity entity) {
        AgentUsageVO vo = new AgentUsageVO();
        vo.setId(entity.getId());
        vo.setUsageDate(entity.getUsageDate());
        vo.setTenantId(entity.getTenantId());
        vo.setAgentName(entity.getAgentName());
        vo.setRequestCount(entity.getRequestCount());
        vo.setInputTokens(entity.getInputTokens());
        vo.setOutputTokens(entity.getOutputTokens());
        vo.setTotalTokens(entity.getTotalTokens());
        vo.setCost(entity.getCost());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private long nullToZero(Integer value) {
        return value == null ? 0L : value;
    }

    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }
}
