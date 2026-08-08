package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionCommissionDistribution;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionCommissionDistributionMapper;
import com.zhush.union.param.UnionCommissionDistributionParam;
import com.zhush.union.query.UnionCommissionDistributionQuery;
import com.zhush.union.vo.UnionCommissionDistributionVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionCommissionDistributionService
 * @Description 佣金分配明细表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionCommissionDistributionService {

    private final UnionCommissionDistributionMapper commissionDistributionMapper;
    private final Converter converter;

    public UnionCommissionDistributionService(UnionCommissionDistributionMapper commissionDistributionMapper, Converter converter) {
        this.commissionDistributionMapper = commissionDistributionMapper;
        this.converter = converter;
    }

    /**
     * 新增佣金分配明细
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createCommissionDistribution(UnionCommissionDistributionParam param) {
        UnionCommissionDistribution entity = converter.convert(param, UnionCommissionDistribution.class);
        commissionDistributionMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改佣金分配明细
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateCommissionDistribution(UnionCommissionDistributionParam param) {
        UnionCommissionDistribution existing = commissionDistributionMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.COMMISSION_DISTRIBUTION_NOT_FOUND, param.getId());
        UnionCommissionDistribution update = converter.convert(param, UnionCommissionDistribution.class);
        commissionDistributionMapper.update(update);
    }

    /**
     * 删除佣金分配明细（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCommissionDistribution(Long id) {
        UnionCommissionDistribution existing = commissionDistributionMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.COMMISSION_DISTRIBUTION_NOT_FOUND, id);
        commissionDistributionMapper.deleteById(id);
    }

    /**
     * 佣金分配明细详情
     */
    public UnionCommissionDistributionVO getCommissionDistribution(Long id) {
        UnionCommissionDistribution entity = commissionDistributionMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.COMMISSION_DISTRIBUTION_NOT_FOUND, id);
        return converter.convert(entity, UnionCommissionDistributionVO.class);
    }

    /**
     * 佣金分配明细分页
     */
    public PageResult<UnionCommissionDistributionVO> getCommissionDistributionPage(UnionCommissionDistributionQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionCommissionDistribution::getOrderId, query.getOrderId())
                .eq(UnionCommissionDistribution::getCommissionRecordId, query.getCommissionRecordId())
                .eq(UnionCommissionDistribution::getReceiverMemberId, query.getReceiverMemberId())
                .eq(UnionCommissionDistribution::getReceiverLevel, query.getReceiverLevel())
                .eq(UnionCommissionDistribution::getDistributionType, query.getDistributionType())
                .eq(UnionCommissionDistribution::getStatus, query.getStatus())
        ;
        Page<UnionCommissionDistribution> page = commissionDistributionMapper.selectPageX(query, queryWrapper);
        List<UnionCommissionDistributionVO> list = converter.convert(page.getRecords(), UnionCommissionDistributionVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
