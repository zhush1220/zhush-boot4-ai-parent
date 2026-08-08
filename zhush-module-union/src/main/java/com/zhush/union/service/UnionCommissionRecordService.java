package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionCommissionRecord;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionCommissionRecordMapper;
import com.zhush.union.param.UnionCommissionRecordParam;
import com.zhush.union.query.UnionCommissionRecordQuery;
import com.zhush.union.vo.UnionCommissionRecordVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionCommissionRecordService
 * @Description 佣金记录表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionCommissionRecordService {

    private final UnionCommissionRecordMapper commissionRecordMapper;
    private final Converter converter;

    public UnionCommissionRecordService(UnionCommissionRecordMapper commissionRecordMapper, Converter converter) {
        this.commissionRecordMapper = commissionRecordMapper;
        this.converter = converter;
    }

    /**
     * 新增佣金记录
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createCommissionRecord(UnionCommissionRecordParam param) {
        UnionCommissionRecord entity = converter.convert(param, UnionCommissionRecord.class);
        commissionRecordMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改佣金记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateCommissionRecord(UnionCommissionRecordParam param) {
        UnionCommissionRecord existing = commissionRecordMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.COMMISSION_RECORD_NOT_FOUND, param.getId());
        UnionCommissionRecord update = converter.convert(param, UnionCommissionRecord.class);
        commissionRecordMapper.update(update);
    }

    /**
     * 删除佣金记录（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCommissionRecord(Long id) {
        UnionCommissionRecord existing = commissionRecordMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.COMMISSION_RECORD_NOT_FOUND, id);
        commissionRecordMapper.deleteById(id);
    }

    /**
     * 佣金记录详情
     */
    public UnionCommissionRecordVO getCommissionRecord(Long id) {
        UnionCommissionRecord entity = commissionRecordMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.COMMISSION_RECORD_NOT_FOUND, id);
        return converter.convert(entity, UnionCommissionRecordVO.class);
    }

    /**
     * 佣金记录分页
     */
    public PageResult<UnionCommissionRecordVO> getCommissionRecordPage(UnionCommissionRecordQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionCommissionRecord::getOrderId, query.getOrderId())
                .eq(UnionCommissionRecord::getMemberId, query.getMemberId())
                .eq(UnionCommissionRecord::getActualPromoterId, query.getActualPromoterId())
                .eq(UnionCommissionRecord::getActualPromoterType, query.getActualPromoterType())
                .eq(UnionCommissionRecord::getExternalPromoterId, query.getExternalPromoterId())
                .eq(UnionCommissionRecord::getSettleTargetType, query.getSettleTargetType())
                .eq(UnionCommissionRecord::getProductId, query.getProductId())
                .eq(UnionCommissionRecord::getStatus, query.getStatus())
                .eq(UnionCommissionRecord::getConfirmStatus, query.getConfirmStatus())
                .like(UnionCommissionRecord::getSettleBatchNo, query.getSettleBatchNo())
                .like(UnionCommissionRecord::getTransactionId, query.getTransactionId())
                .like(UnionCommissionRecord::getRemark, query.getRemark())
                .eq(UnionCommissionRecord::getRetryCount, query.getRetryCount())
        ;
        Page<UnionCommissionRecord> page = commissionRecordMapper.selectPageX(query, queryWrapper);
        List<UnionCommissionRecordVO> list = converter.convert(page.getRecords(), UnionCommissionRecordVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
