package com.zhush.system.mapper;

import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import com.zhush.system.entity.SysDatasource;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 数据源配置 Mapper
 *
 * @author zhush
 * @since 2026/7/12
 */
@Mapper
public interface SysDatasourceMapper extends BaseMapperX<SysDatasource> {

    /**
     * 查询所有启用的数据源
     */
    default List<SysDatasource> selectEnabledList() {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysDatasource::getStatus).eq(1);
            return selectListByQuery(queryWrapper);
    }
}
