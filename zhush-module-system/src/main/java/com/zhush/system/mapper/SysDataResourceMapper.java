package com.zhush.system.mapper;

import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import com.zhush.system.entity.SysDataResource;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 数据资源登记 Mapper
 *
 * @author zhush
 */
@Mapper
public interface SysDataResourceMapper extends BaseMapperX<SysDataResource> {

    /**
     * 按资源编码查询
     */
    default SysDataResource selectByCode(String code) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysDataResource::getCode).eq(code);
        return selectOneByQuery(queryWrapper);
    }

    /**
     * 全部资源（元数据同步比对用）
     */
    default List<SysDataResource> selectAllResources() {
        return selectListByQuery(QueryWrapper.create().orderBy("code", true));
    }
}
