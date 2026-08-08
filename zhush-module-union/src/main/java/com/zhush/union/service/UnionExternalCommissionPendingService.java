package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionExternalCommissionPending;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionExternalCommissionPendingMapper;
import com.zhush.union.param.UnionExternalCommissionPendingParam;
import com.zhush.union.query.UnionExternalCommissionPendingQuery;
import com.zhush.union.vo.UnionExternalCommissionPendingVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionExternalCommissionPendingService
 * @Description 外部推广者待结算佣金表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionExternalCommissionPendingService {

    private final UnionExternalCommissionPendingMapper externalCommissionPendingMapper;
    private final Converter converter;

    public UnionExternalCommissionPendingService(UnionExternalCommissionPendingMapper externalCommissionPendingMapper, Converter converter) {
        this.externalCommissionPendingMapper = externalCommissionPendingMapper;
        this.converter = converter;
    }

    /**
     * 新增外部推广者待结算佣金
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createExternalCommissionPending(UnionExternalCommissionPendingParam param) {
        UnionExternalCommissionPending entity = converter.convert(param, UnionExternalCommissionPending.class);
        externalCommissionPendingMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改外部推广者待结算佣金
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateExternalCommissionPending(UnionExternalCommissionPendingParam param) {
        UnionExternalCommissionPending existing = externalCommissionPendingMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.EXTERNAL_COMMISSION_PENDING_NOT_FOUND, param.getId());
        UnionExternalCommissionPending update = converter.convert(param, UnionExternalCommissionPending.class);
        externalCommissionPendingMapper.update(update);
    }

    /**
     * 删除外部推广者待结算佣金（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteExternalCommissionPending(Long id) {
        UnionExternalCommissionPending existing = externalCommissionPendingMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.EXTERNAL_COMMISSION_PENDING_NOT_FOUND, id);
        externalCommissionPendingMapper.deleteById(id);
    }

    /**
     * 外部推广者待结算佣金详情
     */
    public UnionExternalCommissionPendingVO getExternalCommissionPending(Long id) {
        UnionExternalCommissionPending entity = externalCommissionPendingMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.EXTERNAL_COMMISSION_PENDING_NOT_FOUND, id);
        return converter.convert(entity, UnionExternalCommissionPendingVO.class);
    }

    /**
     * 外部推广者待结算佣金分页
     */
    public PageResult<UnionExternalCommissionPendingVO> getExternalCommissionPendingPage(UnionExternalCommissionPendingQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionExternalCommissionPending::getExternalPromoterId, query.getExternalPromoterId())
                .eq(UnionExternalCommissionPending::getMasterMemberId, query.getMasterMemberId())
                .eq(UnionExternalCommissionPending::getOrderId, query.getOrderId())
                .eq(UnionExternalCommissionPending::getSettleStatus, query.getSettleStatus())
                .like(UnionExternalCommissionPending::getRemark, query.getRemark())
        ;
        Page<UnionExternalCommissionPending> page = externalCommissionPendingMapper.selectPageX(query, queryWrapper);
        List<UnionExternalCommissionPendingVO> list = converter.convert(page.getRecords(), UnionExternalCommissionPendingVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
