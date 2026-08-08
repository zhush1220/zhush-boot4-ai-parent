package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionProductStatusLog;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionProductStatusLogMapper;
import com.zhush.union.param.UnionProductStatusLogParam;
import com.zhush.union.query.UnionProductStatusLogQuery;
import com.zhush.union.vo.UnionProductStatusLogVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionProductStatusLogService
 * @Description 商品状态变更日志表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionProductStatusLogService {

    private final UnionProductStatusLogMapper productStatusLogMapper;
    private final Converter converter;

    public UnionProductStatusLogService(UnionProductStatusLogMapper productStatusLogMapper, Converter converter) {
        this.productStatusLogMapper = productStatusLogMapper;
        this.converter = converter;
    }

    /**
     * 新增商品状态变更日志
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createProductStatusLog(UnionProductStatusLogParam param) {
        UnionProductStatusLog entity = converter.convert(param, UnionProductStatusLog.class);
        productStatusLogMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改商品状态变更日志
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProductStatusLog(UnionProductStatusLogParam param) {
        UnionProductStatusLog existing = productStatusLogMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.PRODUCT_STATUS_LOG_NOT_FOUND, param.getId());
        UnionProductStatusLog update = converter.convert(param, UnionProductStatusLog.class);
        productStatusLogMapper.update(update);
    }

    /**
     * 删除商品状态变更日志（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteProductStatusLog(Long id) {
        UnionProductStatusLog existing = productStatusLogMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.PRODUCT_STATUS_LOG_NOT_FOUND, id);
        productStatusLogMapper.deleteById(id);
    }

    /**
     * 商品状态变更日志详情
     */
    public UnionProductStatusLogVO getProductStatusLog(Long id) {
        UnionProductStatusLog entity = productStatusLogMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.PRODUCT_STATUS_LOG_NOT_FOUND, id);
        return converter.convert(entity, UnionProductStatusLogVO.class);
    }

    /**
     * 商品状态变更日志分页
     */
    public PageResult<UnionProductStatusLogVO> getProductStatusLogPage(UnionProductStatusLogQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionProductStatusLog::getProductId, query.getProductId())
                .like(UnionProductStatusLog::getFieldName, query.getFieldName())
                .like(UnionProductStatusLog::getOldValue, query.getOldValue())
                .like(UnionProductStatusLog::getNewValue, query.getNewValue())
                .eq(UnionProductStatusLog::getOperatorId, query.getOperatorId())
                .like(UnionProductStatusLog::getOperatorName, query.getOperatorName())
                .like(UnionProductStatusLog::getRemark, query.getRemark())
        ;
        Page<UnionProductStatusLog> page = productStatusLogMapper.selectPageX(query, queryWrapper);
        List<UnionProductStatusLogVO> list = converter.convert(page.getRecords(), UnionProductStatusLogVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
