package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionExternalCommissionDetail;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionExternalCommissionDetailMapper;
import com.zhush.union.param.UnionExternalCommissionDetailParam;
import com.zhush.union.query.UnionExternalCommissionDetailQuery;
import com.zhush.union.vo.UnionExternalCommissionDetailVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionExternalCommissionDetailService
 * @Description 外部推广者佣金明细表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionExternalCommissionDetailService {

    private final UnionExternalCommissionDetailMapper externalCommissionDetailMapper;
    private final Converter converter;

    public UnionExternalCommissionDetailService(UnionExternalCommissionDetailMapper externalCommissionDetailMapper, Converter converter) {
        this.externalCommissionDetailMapper = externalCommissionDetailMapper;
        this.converter = converter;
    }

    /**
     * 新增外部推广者佣金明细
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createExternalCommissionDetail(UnionExternalCommissionDetailParam param) {
        UnionExternalCommissionDetail entity = converter.convert(param, UnionExternalCommissionDetail.class);
        externalCommissionDetailMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改外部推广者佣金明细
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateExternalCommissionDetail(UnionExternalCommissionDetailParam param) {
        UnionExternalCommissionDetail existing = externalCommissionDetailMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.EXTERNAL_COMMISSION_DETAIL_NOT_FOUND, param.getId());
        UnionExternalCommissionDetail update = converter.convert(param, UnionExternalCommissionDetail.class);
        externalCommissionDetailMapper.update(update);
    }

    /**
     * 删除外部推广者佣金明细（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteExternalCommissionDetail(Long id) {
        UnionExternalCommissionDetail existing = externalCommissionDetailMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.EXTERNAL_COMMISSION_DETAIL_NOT_FOUND, id);
        externalCommissionDetailMapper.deleteById(id);
    }

    /**
     * 外部推广者佣金明细详情
     */
    public UnionExternalCommissionDetailVO getExternalCommissionDetail(Long id) {
        UnionExternalCommissionDetail entity = externalCommissionDetailMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.EXTERNAL_COMMISSION_DETAIL_NOT_FOUND, id);
        return converter.convert(entity, UnionExternalCommissionDetailVO.class);
    }

    /**
     * 外部推广者佣金明细分页
     */
    public PageResult<UnionExternalCommissionDetailVO> getExternalCommissionDetailPage(UnionExternalCommissionDetailQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionExternalCommissionDetail::getExternalPromoterId, query.getExternalPromoterId())
                .eq(UnionExternalCommissionDetail::getMasterMemberId, query.getMasterMemberId())
                .eq(UnionExternalCommissionDetail::getOrderId, query.getOrderId())
                .eq(UnionExternalCommissionDetail::getSettleStatus, query.getSettleStatus())
                .like(UnionExternalCommissionDetail::getSettleBatchNo, query.getSettleBatchNo())
                .eq(UnionExternalCommissionDetail::getTransactionId, query.getTransactionId())
                .like(UnionExternalCommissionDetail::getRemark, query.getRemark())
        ;
        Page<UnionExternalCommissionDetail> page = externalCommissionDetailMapper.selectPageX(query, queryWrapper);
        List<UnionExternalCommissionDetailVO> list = converter.convert(page.getRecords(), UnionExternalCommissionDetailVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
