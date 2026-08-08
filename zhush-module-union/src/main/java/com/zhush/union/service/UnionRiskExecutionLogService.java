package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionRiskExecutionLog;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionRiskExecutionLogMapper;
import com.zhush.union.param.UnionRiskExecutionLogParam;
import com.zhush.union.query.UnionRiskExecutionLogQuery;
import com.zhush.union.vo.UnionRiskExecutionLogVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionRiskExecutionLogService
 * @Description 风控执行记录表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionRiskExecutionLogService {

    private final UnionRiskExecutionLogMapper riskExecutionLogMapper;
    private final Converter converter;

    public UnionRiskExecutionLogService(UnionRiskExecutionLogMapper riskExecutionLogMapper, Converter converter) {
        this.riskExecutionLogMapper = riskExecutionLogMapper;
        this.converter = converter;
    }

    /**
     * 新增风控执行记录
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createRiskExecutionLog(UnionRiskExecutionLogParam param) {
        UnionRiskExecutionLog entity = converter.convert(param, UnionRiskExecutionLog.class);
        riskExecutionLogMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改风控执行记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateRiskExecutionLog(UnionRiskExecutionLogParam param) {
        UnionRiskExecutionLog existing = riskExecutionLogMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.RISK_EXECUTION_LOG_NOT_FOUND, param.getId());
        UnionRiskExecutionLog update = converter.convert(param, UnionRiskExecutionLog.class);
        riskExecutionLogMapper.update(update);
    }

    /**
     * 删除风控执行记录（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRiskExecutionLog(Long id) {
        UnionRiskExecutionLog existing = riskExecutionLogMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.RISK_EXECUTION_LOG_NOT_FOUND, id);
        riskExecutionLogMapper.deleteById(id);
    }

    /**
     * 风控执行记录详情
     */
    public UnionRiskExecutionLogVO getRiskExecutionLog(Long id) {
        UnionRiskExecutionLog entity = riskExecutionLogMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.RISK_EXECUTION_LOG_NOT_FOUND, id);
        return converter.convert(entity, UnionRiskExecutionLogVO.class);
    }

    /**
     * 风控执行记录分页
     */
    public PageResult<UnionRiskExecutionLogVO> getRiskExecutionLogPage(UnionRiskExecutionLogQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionRiskExecutionLog::getApplyId, query.getApplyId())
                .eq(UnionRiskExecutionLog::getMemberId, query.getMemberId())
                .like(UnionRiskExecutionLog::getContextData, query.getContextData())
                .like(UnionRiskExecutionLog::getMatchedRules, query.getMatchedRules())
                .eq(UnionRiskExecutionLog::getTotalRiskScore, query.getTotalRiskScore())
                .eq(UnionRiskExecutionLog::getDecision, query.getDecision())
                .like(UnionRiskExecutionLog::getDecisionReason, query.getDecisionReason())
                .eq(UnionRiskExecutionLog::getExecutionDuration, query.getExecutionDuration())
        ;
        Page<UnionRiskExecutionLog> page = riskExecutionLogMapper.selectPageX(query, queryWrapper);
        List<UnionRiskExecutionLogVO> list = converter.convert(page.getRecords(), UnionRiskExecutionLogVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
