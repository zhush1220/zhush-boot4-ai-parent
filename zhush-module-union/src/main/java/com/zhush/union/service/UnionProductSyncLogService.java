package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionProductSyncLog;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionProductSyncLogMapper;
import com.zhush.union.param.UnionProductSyncLogParam;
import com.zhush.union.query.UnionProductSyncLogQuery;
import com.zhush.union.vo.UnionProductSyncLogVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionProductSyncLogService
 * @Description 商品同步日志表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionProductSyncLogService {

    private final UnionProductSyncLogMapper productSyncLogMapper;
    private final Converter converter;

    public UnionProductSyncLogService(UnionProductSyncLogMapper productSyncLogMapper, Converter converter) {
        this.productSyncLogMapper = productSyncLogMapper;
        this.converter = converter;
    }

    /**
     * 新增商品同步日志
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createProductSyncLog(UnionProductSyncLogParam param) {
        UnionProductSyncLog entity = converter.convert(param, UnionProductSyncLog.class);
        productSyncLogMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改商品同步日志
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProductSyncLog(UnionProductSyncLogParam param) {
        UnionProductSyncLog existing = productSyncLogMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.PRODUCT_SYNC_LOG_NOT_FOUND, param.getId());
        UnionProductSyncLog update = converter.convert(param, UnionProductSyncLog.class);
        productSyncLogMapper.update(update);
    }

    /**
     * 删除商品同步日志（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteProductSyncLog(Long id) {
        UnionProductSyncLog existing = productSyncLogMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.PRODUCT_SYNC_LOG_NOT_FOUND, id);
        productSyncLogMapper.deleteById(id);
    }

    /**
     * 商品同步日志详情
     */
    public UnionProductSyncLogVO getProductSyncLog(Long id) {
        UnionProductSyncLog entity = productSyncLogMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.PRODUCT_SYNC_LOG_NOT_FOUND, id);
        return converter.convert(entity, UnionProductSyncLogVO.class);
    }

    /**
     * 商品同步日志分页
     */
    public PageResult<UnionProductSyncLogVO> getProductSyncLogPage(UnionProductSyncLogQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionProductSyncLog::getPlatform, query.getPlatform())
                .eq(UnionProductSyncLog::getSyncType, query.getSyncType())
                .eq(UnionProductSyncLog::getTotalCount, query.getTotalCount())
                .eq(UnionProductSyncLog::getSuccessCount, query.getSuccessCount())
                .eq(UnionProductSyncLog::getFailCount, query.getFailCount())
                .like(UnionProductSyncLog::getErrorMsg, query.getErrorMsg())
                .eq(UnionProductSyncLog::getStatus, query.getStatus())
        ;
        Page<UnionProductSyncLog> page = productSyncLogMapper.selectPageX(query, queryWrapper);
        List<UnionProductSyncLogVO> list = converter.convert(page.getRecords(), UnionProductSyncLogVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
