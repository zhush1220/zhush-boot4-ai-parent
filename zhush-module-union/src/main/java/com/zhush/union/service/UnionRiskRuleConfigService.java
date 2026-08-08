package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionRiskRuleConfig;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionRiskRuleConfigMapper;
import com.zhush.union.param.UnionRiskRuleConfigParam;
import com.zhush.union.query.UnionRiskRuleConfigQuery;
import com.zhush.union.vo.UnionRiskRuleConfigVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionRiskRuleConfigService
 * @Description 风控规则配置表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionRiskRuleConfigService {

    private final UnionRiskRuleConfigMapper riskRuleConfigMapper;
    private final Converter converter;

    public UnionRiskRuleConfigService(UnionRiskRuleConfigMapper riskRuleConfigMapper, Converter converter) {
        this.riskRuleConfigMapper = riskRuleConfigMapper;
        this.converter = converter;
    }

    /**
     * 新增风控规则配置
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createRiskRuleConfig(UnionRiskRuleConfigParam param) {
        UnionRiskRuleConfig entity = converter.convert(param, UnionRiskRuleConfig.class);
        riskRuleConfigMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改风控规则配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateRiskRuleConfig(UnionRiskRuleConfigParam param) {
        UnionRiskRuleConfig existing = riskRuleConfigMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.RISK_RULE_CONFIG_NOT_FOUND, param.getId());
        UnionRiskRuleConfig update = converter.convert(param, UnionRiskRuleConfig.class);
        riskRuleConfigMapper.update(update);
    }

    /**
     * 删除风控规则配置（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRiskRuleConfig(Long id) {
        UnionRiskRuleConfig existing = riskRuleConfigMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.RISK_RULE_CONFIG_NOT_FOUND, id);
        riskRuleConfigMapper.deleteById(id);
    }

    /**
     * 风控规则配置详情
     */
    public UnionRiskRuleConfigVO getRiskRuleConfig(Long id) {
        UnionRiskRuleConfig entity = riskRuleConfigMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.RISK_RULE_CONFIG_NOT_FOUND, id);
        return converter.convert(entity, UnionRiskRuleConfigVO.class);
    }

    /**
     * 风控规则配置分页
     */
    public PageResult<UnionRiskRuleConfigVO> getRiskRuleConfigPage(UnionRiskRuleConfigQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(UnionRiskRuleConfig::getRuleCode, query.getRuleCode())
                .like(UnionRiskRuleConfig::getRuleName, query.getRuleName())
                .eq(UnionRiskRuleConfig::getRuleCategory, query.getRuleCategory())
                .eq(UnionRiskRuleConfig::getRuleType, query.getRuleType())
                .like(UnionRiskRuleConfig::getConditionExpression, query.getConditionExpression())
                .like(UnionRiskRuleConfig::getConditionSql, query.getConditionSql())
                .eq(UnionRiskRuleConfig::getActionType, query.getActionType())
                .like(UnionRiskRuleConfig::getActionParams, query.getActionParams())
                .eq(UnionRiskRuleConfig::getRiskScore, query.getRiskScore())
                .eq(UnionRiskRuleConfig::getAutoAuditThreshold, query.getAutoAuditThreshold())
                .eq(UnionRiskRuleConfig::getPriority, query.getPriority())
                .eq(UnionRiskRuleConfig::getStatus, query.getStatus())
                .like(UnionRiskRuleConfig::getDescription, query.getDescription())
        ;
        Page<UnionRiskRuleConfig> page = riskRuleConfigMapper.selectPageX(query, queryWrapper);
        List<UnionRiskRuleConfigVO> list = converter.convert(page.getRecords(), UnionRiskRuleConfigVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
