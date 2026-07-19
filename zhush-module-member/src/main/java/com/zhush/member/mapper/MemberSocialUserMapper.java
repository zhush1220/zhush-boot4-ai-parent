package com.zhush.member.mapper;

import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.member.entity.MemberSocialUser;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员第三方授权 Mapper
 *
 * @author zhush
 */
@Mapper
public interface MemberSocialUserMapper extends BaseMapperX<MemberSocialUser> {

    /**
     * 按第三方类型 + openId 查询绑定
     */
    default MemberSocialUser selectByTypeAndOpenId(String socialType, String openId) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(MemberSocialUser::getSocialType).eq(socialType)
                .and(MemberSocialUser::getOpenId).eq(openId);
        return selectOneByQuery(queryWrapper);
    }
}
