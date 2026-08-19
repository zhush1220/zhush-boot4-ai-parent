package com.zhush.system.mapper;

import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import com.zhush.system.entity.SysDictData;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 字典数据 Mapper
 *
 * @author zhush
 */
@Mapper
public interface SysDictDataMapper extends BaseMapperX<SysDictData> {

    /**
     * 按字典类型编码查询启用中的字典数据（按 sort 升序）
     */
    default List<SysDictData> selectEnabledListByDictType(String dictType) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysDictData::getDictType).eq(dictType)
                .and(SysDictData::getStatus).eq(1);
        queryWrapper.orderBy(SysDictData::getSort, true);
        return selectListByQuery(queryWrapper);
    }

    /**
     * 按字典类型编码查询全部字典数据（含禁用，按 sort 升序）
     */
    default List<SysDictData> selectListByDictType(String dictType) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysDictData::getDictType).eq(dictType);
        queryWrapper.orderBy(SysDictData::getSort, true);
        return selectListByQuery(queryWrapper);
    }

    /**
     * 按字典类型编码 + 键值查询（唯一性校验）
     */
    default SysDictData selectByDictTypeAndValue(String dictType, String value) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysDictData::getDictType).eq(dictType)
                .and(SysDictData::getValue).eq(value);
        return selectOneByQuery(queryWrapper);
    }

    /**
     * 统计字典类型下的数据条数
     */
    default long countByDictType(String dictType) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysDictData::getDictType).eq(dictType);
        return selectCountByQuery(queryWrapper);
    }
}
