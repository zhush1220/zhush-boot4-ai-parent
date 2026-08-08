package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionSettleBatch;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionSettleBatchMapper;
import com.zhush.union.param.UnionSettleBatchParam;
import com.zhush.union.query.UnionSettleBatchQuery;
import com.zhush.union.vo.UnionSettleBatchVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionSettleBatchService
 * @Description 结算批次表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionSettleBatchService {

    private final UnionSettleBatchMapper settleBatchMapper;
    private final Converter converter;

    public UnionSettleBatchService(UnionSettleBatchMapper settleBatchMapper, Converter converter) {
        this.settleBatchMapper = settleBatchMapper;
        this.converter = converter;
    }

    /**
     * 新增结算批次
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createSettleBatch(UnionSettleBatchParam param) {
        UnionSettleBatch entity = converter.convert(param, UnionSettleBatch.class);
        settleBatchMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改结算批次
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateSettleBatch(UnionSettleBatchParam param) {
        UnionSettleBatch existing = settleBatchMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.SETTLE_BATCH_NOT_FOUND, param.getId());
        UnionSettleBatch update = converter.convert(param, UnionSettleBatch.class);
        settleBatchMapper.update(update);
    }

    /**
     * 删除结算批次（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSettleBatch(Long id) {
        UnionSettleBatch existing = settleBatchMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.SETTLE_BATCH_NOT_FOUND, id);
        settleBatchMapper.deleteById(id);
    }

    /**
     * 结算批次详情
     */
    public UnionSettleBatchVO getSettleBatch(Long id) {
        UnionSettleBatch entity = settleBatchMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.SETTLE_BATCH_NOT_FOUND, id);
        return converter.convert(entity, UnionSettleBatchVO.class);
    }

    /**
     * 结算批次分页
     */
    public PageResult<UnionSettleBatchVO> getSettleBatchPage(UnionSettleBatchQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(UnionSettleBatch::getBatchNo, query.getBatchNo())
                .eq(UnionSettleBatch::getRecordCount, query.getRecordCount())
                .eq(UnionSettleBatch::getStatus, query.getStatus())
                .eq(UnionSettleBatch::getOperatorId, query.getOperatorId())
                .like(UnionSettleBatch::getRemark, query.getRemark())
        ;
        Page<UnionSettleBatch> page = settleBatchMapper.selectPageX(query, queryWrapper);
        List<UnionSettleBatchVO> list = converter.convert(page.getRecords(), UnionSettleBatchVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
