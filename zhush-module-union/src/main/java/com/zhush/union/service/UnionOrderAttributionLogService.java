package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionOrderAttributionLog;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionOrderAttributionLogMapper;
import com.zhush.union.param.UnionOrderAttributionLogParam;
import com.zhush.union.query.UnionOrderAttributionLogQuery;
import com.zhush.union.vo.UnionOrderAttributionLogVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionOrderAttributionLogService
 * @Description 订单归因日志表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionOrderAttributionLogService {

    private final UnionOrderAttributionLogMapper orderAttributionLogMapper;
    private final Converter converter;

    public UnionOrderAttributionLogService(UnionOrderAttributionLogMapper orderAttributionLogMapper, Converter converter) {
        this.orderAttributionLogMapper = orderAttributionLogMapper;
        this.converter = converter;
    }

    /**
     * 新增订单归因日志
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createOrderAttributionLog(UnionOrderAttributionLogParam param) {
        UnionOrderAttributionLog entity = converter.convert(param, UnionOrderAttributionLog.class);
        orderAttributionLogMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改订单归因日志
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderAttributionLog(UnionOrderAttributionLogParam param) {
        UnionOrderAttributionLog existing = orderAttributionLogMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.ORDER_ATTRIBUTION_LOG_NOT_FOUND, param.getId());
        UnionOrderAttributionLog update = converter.convert(param, UnionOrderAttributionLog.class);
        orderAttributionLogMapper.update(update);
    }

    /**
     * 删除订单归因日志（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrderAttributionLog(Long id) {
        UnionOrderAttributionLog existing = orderAttributionLogMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.ORDER_ATTRIBUTION_LOG_NOT_FOUND, id);
        orderAttributionLogMapper.deleteById(id);
    }

    /**
     * 订单归因日志详情
     */
    public UnionOrderAttributionLogVO getOrderAttributionLog(Long id) {
        UnionOrderAttributionLog entity = orderAttributionLogMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.ORDER_ATTRIBUTION_LOG_NOT_FOUND, id);
        return converter.convert(entity, UnionOrderAttributionLogVO.class);
    }

    /**
     * 订单归因日志分页
     */
    public PageResult<UnionOrderAttributionLogVO> getOrderAttributionLogPage(UnionOrderAttributionLogQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionOrderAttributionLog::getOrderId, query.getOrderId())
                .eq(UnionOrderAttributionLog::getPlatform, query.getPlatform())
                .like(UnionOrderAttributionLog::getOuterOrderId, query.getOuterOrderId())
                .eq(UnionOrderAttributionLog::getAttributedMemberId, query.getAttributedMemberId())
                .like(UnionOrderAttributionLog::getAttributionMethod, query.getAttributionMethod())
                .eq(UnionOrderAttributionLog::getProcessStatus, query.getProcessStatus())
                .like(UnionOrderAttributionLog::getErrorMsg, query.getErrorMsg())
        ;
        Page<UnionOrderAttributionLog> page = orderAttributionLogMapper.selectPageX(query, queryWrapper);
        List<UnionOrderAttributionLogVO> list = converter.convert(page.getRecords(), UnionOrderAttributionLogVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
