package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionWithdrawWhitelist;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionWithdrawWhitelistMapper;
import com.zhush.union.param.UnionWithdrawWhitelistParam;
import com.zhush.union.query.UnionWithdrawWhitelistQuery;
import com.zhush.union.vo.UnionWithdrawWhitelistVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionWithdrawWhitelistService
 * @Description 提现白名单表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionWithdrawWhitelistService {

    private final UnionWithdrawWhitelistMapper withdrawWhitelistMapper;
    private final Converter converter;

    public UnionWithdrawWhitelistService(UnionWithdrawWhitelistMapper withdrawWhitelistMapper, Converter converter) {
        this.withdrawWhitelistMapper = withdrawWhitelistMapper;
        this.converter = converter;
    }

    /**
     * 新增提现白名单
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createWithdrawWhitelist(UnionWithdrawWhitelistParam param) {
        UnionWithdrawWhitelist entity = converter.convert(param, UnionWithdrawWhitelist.class);
        withdrawWhitelistMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改提现白名单
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateWithdrawWhitelist(UnionWithdrawWhitelistParam param) {
        UnionWithdrawWhitelist existing = withdrawWhitelistMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.WITHDRAW_WHITELIST_NOT_FOUND, param.getId());
        UnionWithdrawWhitelist update = converter.convert(param, UnionWithdrawWhitelist.class);
        withdrawWhitelistMapper.update(update);
    }

    /**
     * 删除提现白名单（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteWithdrawWhitelist(Long id) {
        UnionWithdrawWhitelist existing = withdrawWhitelistMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.WITHDRAW_WHITELIST_NOT_FOUND, id);
        withdrawWhitelistMapper.deleteById(id);
    }

    /**
     * 提现白名单详情
     */
    public UnionWithdrawWhitelistVO getWithdrawWhitelist(Long id) {
        UnionWithdrawWhitelist entity = withdrawWhitelistMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.WITHDRAW_WHITELIST_NOT_FOUND, id);
        return converter.convert(entity, UnionWithdrawWhitelistVO.class);
    }

    /**
     * 提现白名单分页
     */
    public PageResult<UnionWithdrawWhitelistVO> getWithdrawWhitelistPage(UnionWithdrawWhitelistQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionWithdrawWhitelist::getMemberId, query.getMemberId())
                .eq(UnionWithdrawWhitelist::getWhitelistType, query.getWhitelistType())
                .like(UnionWithdrawWhitelist::getReason, query.getReason())
                .eq(UnionWithdrawWhitelist::getOperatorId, query.getOperatorId())
                .eq(UnionWithdrawWhitelist::getStatus, query.getStatus())
        ;
        Page<UnionWithdrawWhitelist> page = withdrawWhitelistMapper.selectPageX(query, queryWrapper);
        List<UnionWithdrawWhitelistVO> list = converter.convert(page.getRecords(), UnionWithdrawWhitelistVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
