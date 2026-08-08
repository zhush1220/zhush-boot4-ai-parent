package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionCommissionAdjustment;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionCommissionAdjustmentMapper;
import com.zhush.union.param.UnionCommissionAdjustmentParam;
import com.zhush.union.query.UnionCommissionAdjustmentQuery;
import com.zhush.union.vo.UnionCommissionAdjustmentVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionCommissionAdjustmentService
 * @Description 佣金调整记录表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionCommissionAdjustmentService {

    private final UnionCommissionAdjustmentMapper commissionAdjustmentMapper;
    private final Converter converter;

    public UnionCommissionAdjustmentService(UnionCommissionAdjustmentMapper commissionAdjustmentMapper, Converter converter) {
        this.commissionAdjustmentMapper = commissionAdjustmentMapper;
        this.converter = converter;
    }

    /**
     * 新增佣金调整记录
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createCommissionAdjustment(UnionCommissionAdjustmentParam param) {
        UnionCommissionAdjustment entity = converter.convert(param, UnionCommissionAdjustment.class);
        commissionAdjustmentMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改佣金调整记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateCommissionAdjustment(UnionCommissionAdjustmentParam param) {
        UnionCommissionAdjustment existing = commissionAdjustmentMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.COMMISSION_ADJUSTMENT_NOT_FOUND, param.getId());
        UnionCommissionAdjustment update = converter.convert(param, UnionCommissionAdjustment.class);
        commissionAdjustmentMapper.update(update);
    }

    /**
     * 删除佣金调整记录（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCommissionAdjustment(Long id) {
        UnionCommissionAdjustment existing = commissionAdjustmentMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.COMMISSION_ADJUSTMENT_NOT_FOUND, id);
        commissionAdjustmentMapper.deleteById(id);
    }

    /**
     * 佣金调整记录详情
     */
    public UnionCommissionAdjustmentVO getCommissionAdjustment(Long id) {
        UnionCommissionAdjustment entity = commissionAdjustmentMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.COMMISSION_ADJUSTMENT_NOT_FOUND, id);
        return converter.convert(entity, UnionCommissionAdjustmentVO.class);
    }

    /**
     * 佣金调整记录分页
     */
    public PageResult<UnionCommissionAdjustmentVO> getCommissionAdjustmentPage(UnionCommissionAdjustmentQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionCommissionAdjustment::getMemberId, query.getMemberId())
                .like(UnionCommissionAdjustment::getAdjustNo, query.getAdjustNo())
                .eq(UnionCommissionAdjustment::getAdjustType, query.getAdjustType())
                .eq(UnionCommissionAdjustment::getOrderId, query.getOrderId())
                .like(UnionCommissionAdjustment::getReason, query.getReason())
                .like(UnionCommissionAdjustment::getAttachments, query.getAttachments())
                .eq(UnionCommissionAdjustment::getOperatorId, query.getOperatorId())
                .like(UnionCommissionAdjustment::getOperatorName, query.getOperatorName())
                .eq(UnionCommissionAdjustment::getStatus, query.getStatus())
        ;
        Page<UnionCommissionAdjustment> page = commissionAdjustmentMapper.selectPageX(query, queryWrapper);
        List<UnionCommissionAdjustmentVO> list = converter.convert(page.getRecords(), UnionCommissionAdjustmentVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
