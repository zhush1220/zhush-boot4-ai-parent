package com.zhush.system.mapper;

import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import com.zhush.system.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色 Mapper
 *
 * @author zhush
 */
@Mapper
public interface SysRoleMapper extends BaseMapperX<SysRole> {

    /**
     * 按角色编码查询
     */
    default SysRole selectByCode(String code) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysRole::getCode).eq(code);
        return selectOneByQuery(queryWrapper);
    }
}
