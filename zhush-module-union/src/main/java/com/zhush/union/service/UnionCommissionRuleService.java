package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionCommissionRule;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionCommissionRuleMapper;
import com.zhush.union.param.UnionCommissionRuleParam;
import com.zhush.union.query.UnionCommissionRuleQuery;
import com.zhush.union.vo.UnionCommissionRuleVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionCommissionRuleService
 * @Description 佣金分配规则配置表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionCommissionRuleService {

    private final UnionCommissionRuleMapper commissionRuleMapper;
    private final Converter converter;

    public UnionCommissionRuleService(UnionCommissionRuleMapper commissionRuleMapper, Converter converter) {
        this.commissionRuleMapper = commissionRuleMapper;
        this.converter = converter;
    }

    /**
     * 新增佣金分配规则配置
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createCommissionRule(UnionCommissionRuleParam param) {
        UnionCommissionRule entity = converter.convert(param, UnionCommissionRule.class);
        commissionRuleMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改佣金分配规则配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateCommissionRule(UnionCommissionRuleParam param) {
        UnionCommissionRule existing = commissionRuleMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.COMMISSION_RULE_NOT_FOUND, param.getId());
        UnionCommissionRule update = converter.convert(param, UnionCommissionRule.class);
        commissionRuleMapper.update(update);
    }

    /**
     * 删除佣金分配规则配置（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCommissionRule(Long id) {
        UnionCommissionRule existing = commissionRuleMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.COMMISSION_RULE_NOT_FOUND, id);
        commissionRuleMapper.deleteById(id);
    }

    /**
     * 佣金分配规则配置详情
     */
    public UnionCommissionRuleVO getCommissionRule(Long id) {
        UnionCommissionRule entity = commissionRuleMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.COMMISSION_RULE_NOT_FOUND, id);
        return converter.convert(entity, UnionCommissionRuleVO.class);
    }

    /**
     * 佣金分配规则配置分页
     */
    public PageResult<UnionCommissionRuleVO> getCommissionRulePage(UnionCommissionRuleQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(UnionCommissionRule::getRuleName, query.getRuleName())
                .eq(UnionCommissionRule::getRuleType, query.getRuleType())
                .eq(UnionCommissionRule::getPlatform, query.getPlatform())
                .eq(UnionCommissionRule::getCategoryId, query.getCategoryId())
                .eq(UnionCommissionRule::getMemberLevel, query.getMemberLevel())
                .eq(UnionCommissionRule::getPriority, query.getPriority())
                .eq(UnionCommissionRule::getStatus, query.getStatus())
                .like(UnionCommissionRule::getRemark, query.getRemark())
        ;
        Page<UnionCommissionRule> page = commissionRuleMapper.selectPageX(query, queryWrapper);
        List<UnionCommissionRuleVO> list = converter.convert(page.getRecords(), UnionCommissionRuleVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
