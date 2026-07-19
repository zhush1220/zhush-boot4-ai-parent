package com.zhush.system.mapper;

import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import com.zhush.system.entity.SysSocialUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理端第三方登录绑定 Mapper
 *
 * @author zhush
 */
@Mapper
public interface SysSocialUserMapper extends BaseMapperX<SysSocialUser> {

    /**
     * 按第三方类型 + openId 查询绑定
     */
    default SysSocialUser selectByTypeAndOpenId(String socialType, String openId) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysSocialUser::getSocialType).eq(socialType)
                .and(SysSocialUser::getOpenId).eq(openId);
        return selectOneByQuery(queryWrapper);
    }
}
