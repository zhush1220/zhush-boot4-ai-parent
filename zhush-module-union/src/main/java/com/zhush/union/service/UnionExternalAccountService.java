package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionExternalAccount;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionExternalAccountMapper;
import com.zhush.union.param.UnionExternalAccountParam;
import com.zhush.union.query.UnionExternalAccountQuery;
import com.zhush.union.vo.UnionExternalAccountVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionExternalAccountService
 * @Description 外部推广者账户表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionExternalAccountService {

    private final UnionExternalAccountMapper externalAccountMapper;
    private final Converter converter;

    public UnionExternalAccountService(UnionExternalAccountMapper externalAccountMapper, Converter converter) {
        this.externalAccountMapper = externalAccountMapper;
        this.converter = converter;
    }

    /**
     * 新增外部推广者账户
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createExternalAccount(UnionExternalAccountParam param) {
        UnionExternalAccount entity = converter.convert(param, UnionExternalAccount.class);
        externalAccountMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改外部推广者账户
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateExternalAccount(UnionExternalAccountParam param) {
        UnionExternalAccount existing = externalAccountMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.EXTERNAL_ACCOUNT_NOT_FOUND, param.getId());
        UnionExternalAccount update = converter.convert(param, UnionExternalAccount.class);
        externalAccountMapper.update(update);
    }

    /**
     * 删除外部推广者账户（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteExternalAccount(Long id) {
        UnionExternalAccount existing = externalAccountMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.EXTERNAL_ACCOUNT_NOT_FOUND, id);
        externalAccountMapper.deleteById(id);
    }

    /**
     * 外部推广者账户详情
     */
    public UnionExternalAccountVO getExternalAccount(Long id) {
        UnionExternalAccount entity = externalAccountMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.EXTERNAL_ACCOUNT_NOT_FOUND, id);
        return converter.convert(entity, UnionExternalAccountVO.class);
    }

    /**
     * 外部推广者账户分页
     */
    public PageResult<UnionExternalAccountVO> getExternalAccountPage(UnionExternalAccountQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionExternalAccount::getExternalPromoterId, query.getExternalPromoterId())
                .eq(UnionExternalAccount::getMasterMemberId, query.getMasterMemberId())
                .eq(UnionExternalAccount::getVersion, query.getVersion())
        ;
        Page<UnionExternalAccount> page = externalAccountMapper.selectPageX(query, queryWrapper);
        List<UnionExternalAccountVO> list = converter.convert(page.getRecords(), UnionExternalAccountVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
