package com.zhush.system.mapper;

import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import com.zhush.system.entity.SysDictType;
import org.apache.ibatis.annotations.Mapper;

/**
 * 字典类型 Mapper
 *
 * @author zhush
 */
@Mapper
public interface SysDictTypeMapper extends BaseMapperX<SysDictType> {

    /**
     * 按字典类型编码查询
     */
    default SysDictType selectByType(String type) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysDictType::getType).eq(type);
        return selectOneByQuery(queryWrapper);
    }
}
