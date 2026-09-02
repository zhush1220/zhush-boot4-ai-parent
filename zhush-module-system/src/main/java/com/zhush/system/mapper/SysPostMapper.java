package com.zhush.system.mapper;

import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import com.zhush.system.entity.SysPost;
import org.apache.ibatis.annotations.Mapper;

/**
 * 岗位 Mapper
 *
 * @author zhush
 */
@Mapper
public interface SysPostMapper extends BaseMapperX<SysPost> {

    /**
     * 按岗位编码查询（编码唯一性校验）
     */
    default SysPost selectByPostCode(String postCode) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysPost::getPostCode).eq(postCode);
        return selectOneByQuery(queryWrapper);
    }
}
