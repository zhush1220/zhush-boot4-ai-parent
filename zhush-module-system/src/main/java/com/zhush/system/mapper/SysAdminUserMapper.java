package com.zhush.system.mapper;

import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import com.zhush.system.entity.SysAdminUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员用户 Mapper
 *
 * @author zhush
 */
@Mapper
public interface SysAdminUserMapper extends BaseMapperX<SysAdminUser> {

    /**
     * 按登录账号查询
     */
    default SysAdminUser selectByUsername(String username) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysAdminUser::getUsername).eq(username);
        return selectOneByQuery(queryWrapper);
    }

    /**
     * 统计部门下的用户数
     */
    default long selectCountByDeptId(Long deptId) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysAdminUser::getDeptId).eq(deptId);
        return selectCountByQuery(queryWrapper);
    }
}
