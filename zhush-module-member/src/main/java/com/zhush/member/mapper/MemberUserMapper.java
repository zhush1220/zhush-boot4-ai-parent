package com.zhush.member.mapper;

import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.member.entity.MemberUser;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员用户 Mapper
 *
 * @author zhush
 */
@Mapper
public interface MemberUserMapper extends BaseMapperX<MemberUser> {

    /**
     * 按手机号查询
     */
    default MemberUser selectByMobile(String mobile) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(MemberUser::getMobile).eq(mobile);
        return selectOneByQuery(queryWrapper);
    }
}
