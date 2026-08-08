package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionMemberAccount;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionMemberAccountMapper;
import com.zhush.union.param.UnionMemberAccountParam;
import com.zhush.union.query.UnionMemberAccountQuery;
import com.zhush.union.vo.UnionMemberAccountVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionMemberAccountService
 * @Description 推广者账户表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionMemberAccountService {

    private final UnionMemberAccountMapper memberAccountMapper;
    private final Converter converter;

    public UnionMemberAccountService(UnionMemberAccountMapper memberAccountMapper, Converter converter) {
        this.memberAccountMapper = memberAccountMapper;
        this.converter = converter;
    }

    /**
     * 新增推广者账户
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createMemberAccount(UnionMemberAccountParam param) {
        UnionMemberAccount entity = converter.convert(param, UnionMemberAccount.class);
        memberAccountMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改推广者账户
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateMemberAccount(UnionMemberAccountParam param) {
        UnionMemberAccount existing = memberAccountMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.MEMBER_ACCOUNT_NOT_FOUND, param.getId());
        UnionMemberAccount update = converter.convert(param, UnionMemberAccount.class);
        memberAccountMapper.update(update);
    }

    /**
     * 删除推广者账户（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteMemberAccount(Long id) {
        UnionMemberAccount existing = memberAccountMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.MEMBER_ACCOUNT_NOT_FOUND, id);
        memberAccountMapper.deleteById(id);
    }

    /**
     * 推广者账户详情
     */
    public UnionMemberAccountVO getMemberAccount(Long id) {
        UnionMemberAccount entity = memberAccountMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.MEMBER_ACCOUNT_NOT_FOUND, id);
        return converter.convert(entity, UnionMemberAccountVO.class);
    }

    /**
     * 推广者账户分页
     */
    public PageResult<UnionMemberAccountVO> getMemberAccountPage(UnionMemberAccountQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionMemberAccount::getMemberId, query.getMemberId())
                .eq(UnionMemberAccount::getVersion, query.getVersion())
        ;
        Page<UnionMemberAccount> page = memberAccountMapper.selectPageX(query, queryWrapper);
        List<UnionMemberAccountVO> list = converter.convert(page.getRecords(), UnionMemberAccountVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
