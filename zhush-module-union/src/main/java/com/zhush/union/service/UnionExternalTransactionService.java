package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionExternalTransaction;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionExternalTransactionMapper;
import com.zhush.union.param.UnionExternalTransactionParam;
import com.zhush.union.query.UnionExternalTransactionQuery;
import com.zhush.union.vo.UnionExternalTransactionVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionExternalTransactionService
 * @Description 外部推广者资金流水表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionExternalTransactionService {

    private final UnionExternalTransactionMapper externalTransactionMapper;
    private final Converter converter;

    public UnionExternalTransactionService(UnionExternalTransactionMapper externalTransactionMapper, Converter converter) {
        this.externalTransactionMapper = externalTransactionMapper;
        this.converter = converter;
    }

    /**
     * 新增外部推广者资金流水
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createExternalTransaction(UnionExternalTransactionParam param) {
        UnionExternalTransaction entity = converter.convert(param, UnionExternalTransaction.class);
        externalTransactionMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改外部推广者资金流水
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateExternalTransaction(UnionExternalTransactionParam param) {
        UnionExternalTransaction existing = externalTransactionMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.EXTERNAL_TRANSACTION_NOT_FOUND, param.getId());
        UnionExternalTransaction update = converter.convert(param, UnionExternalTransaction.class);
        externalTransactionMapper.update(update);
    }

    /**
     * 删除外部推广者资金流水（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteExternalTransaction(Long id) {
        UnionExternalTransaction existing = externalTransactionMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.EXTERNAL_TRANSACTION_NOT_FOUND, id);
        externalTransactionMapper.deleteById(id);
    }

    /**
     * 外部推广者资金流水详情
     */
    public UnionExternalTransactionVO getExternalTransaction(Long id) {
        UnionExternalTransaction entity = externalTransactionMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.EXTERNAL_TRANSACTION_NOT_FOUND, id);
        return converter.convert(entity, UnionExternalTransactionVO.class);
    }

    /**
     * 外部推广者资金流水分页
     */
    public PageResult<UnionExternalTransactionVO> getExternalTransactionPage(UnionExternalTransactionQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionExternalTransaction::getExternalPromoterId, query.getExternalPromoterId())
                .eq(UnionExternalTransaction::getMasterMemberId, query.getMasterMemberId())
                .like(UnionExternalTransaction::getTransactionNo, query.getTransactionNo())
                .eq(UnionExternalTransaction::getTransactionType, query.getTransactionType())
                .like(UnionExternalTransaction::getSourceId, query.getSourceId())
                .like(UnionExternalTransaction::getRemark, query.getRemark())
                .eq(UnionExternalTransaction::getStatus, query.getStatus())
        ;
        Page<UnionExternalTransaction> page = externalTransactionMapper.selectPageX(query, queryWrapper);
        List<UnionExternalTransactionVO> list = converter.convert(page.getRecords(), UnionExternalTransactionVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
