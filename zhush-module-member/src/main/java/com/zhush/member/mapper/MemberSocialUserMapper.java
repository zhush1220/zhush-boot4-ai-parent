package com.zhush.member.mapper;

import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.member.entity.MemberSocialUser;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import java.util.List;
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

    /**
     * 按会员用户ID + 第三方类型反查绑定（用于出款前服务端反查 openid，不信任前端提交）
     */
    default MemberSocialUser selectByUserIdAndType(Long userId, String socialType) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(MemberSocialUser::getUserId).eq(userId)
                .and(MemberSocialUser::getSocialType).eq(socialType);
        List<MemberSocialUser> list = selectListByQuery(queryWrapper);
        return CollUtil.isEmpty(list) ? null : list.get(0);
    }
}
