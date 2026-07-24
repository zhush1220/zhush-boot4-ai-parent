package com.zhush.member.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.member.entity.MemberUser;
import com.zhush.member.enums.MemberErrorCode;
import com.zhush.member.mapper.MemberUserMapper;
import com.zhush.member.query.MemberUserQuery;
import com.zhush.member.vo.MemberUserVO;
import com.zhush.mybatis.core.query.QueryWrapperX;
import io.github.linpeilie.Converter;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * 会员管理服务（管理端查询）
 *
 * @author zhush
 */
@Service
public class MemberUserService {

    private final MemberUserMapper memberUserMapper;
    private final Converter converter;

    public MemberUserService(MemberUserMapper memberUserMapper,
                             Converter converter) {
        this.memberUserMapper = memberUserMapper;
        this.converter = converter;
    }

    /**
     * 会员分页
     */
    public PageResult<MemberUserVO> getMemberPage(MemberUserQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(MemberUser::getMobile, query.getMobile())
                .like(MemberUser::getNickname, query.getNickname())
                .eq(MemberUser::getStatus, query.getStatus())
                .eq(MemberUser::getRegisterType, query.getRegisterType());
        queryWrapper.orderBy("id", false);
        Page<MemberUser> page = memberUserMapper.selectPageX(query, queryWrapper);
        List<MemberUserVO> list = converter.convert(page.getRecords(), MemberUserVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

    /**
     * 会员详情
     */
    public MemberUserVO getMember(Long id) {
        MemberUser member = memberUserMapper.selectOneById(id);
        assertNotNull(member, MemberErrorCode.MEMBER_NOT_FOUND, id);
        return converter.convert(member, MemberUserVO.class);
    }

}
