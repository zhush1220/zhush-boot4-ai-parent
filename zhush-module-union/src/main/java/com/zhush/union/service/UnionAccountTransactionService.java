package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionAccountTransaction;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionAccountTransactionMapper;
import com.zhush.union.param.UnionAccountTransactionParam;
import com.zhush.union.query.UnionAccountTransactionQuery;
import com.zhush.union.vo.UnionAccountTransactionVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionAccountTransactionService
 * @Description 账户资金流水表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionAccountTransactionService {

    private final UnionAccountTransactionMapper accountTransactionMapper;
    private final Converter converter;

    public UnionAccountTransactionService(UnionAccountTransactionMapper accountTransactionMapper, Converter converter) {
        this.accountTransactionMapper = accountTransactionMapper;
        this.converter = converter;
    }

    /**
     * 新增账户资金流水
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createAccountTransaction(UnionAccountTransactionParam param) {
        UnionAccountTransaction entity = converter.convert(param, UnionAccountTransaction.class);
        accountTransactionMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改账户资金流水
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateAccountTransaction(UnionAccountTransactionParam param) {
        UnionAccountTransaction existing = accountTransactionMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.ACCOUNT_TRANSACTION_NOT_FOUND, param.getId());
        UnionAccountTransaction update = converter.convert(param, UnionAccountTransaction.class);
        accountTransactionMapper.update(update);
    }

    /**
     * 删除账户资金流水（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccountTransaction(Long id) {
        UnionAccountTransaction existing = accountTransactionMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.ACCOUNT_TRANSACTION_NOT_FOUND, id);
        accountTransactionMapper.deleteById(id);
    }

    /**
     * 账户资金流水详情
     */
    public UnionAccountTransactionVO getAccountTransaction(Long id) {
        UnionAccountTransaction entity = accountTransactionMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.ACCOUNT_TRANSACTION_NOT_FOUND, id);
        return converter.convert(entity, UnionAccountTransactionVO.class);
    }

    /**
     * 账户资金流水分页
     */
    public PageResult<UnionAccountTransactionVO> getAccountTransactionPage(UnionAccountTransactionQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionAccountTransaction::getMemberId, query.getMemberId())
                .like(UnionAccountTransaction::getTransactionNo, query.getTransactionNo())
                .eq(UnionAccountTransaction::getTransactionType, query.getTransactionType())
                .like(UnionAccountTransaction::getSourceId, query.getSourceId())
                .like(UnionAccountTransaction::getRemark, query.getRemark())
                .eq(UnionAccountTransaction::getStatus, query.getStatus())
        ;
        Page<UnionAccountTransaction> page = accountTransactionMapper.selectPageX(query, queryWrapper);
        List<UnionAccountTransactionVO> list = converter.convert(page.getRecords(), UnionAccountTransactionVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
