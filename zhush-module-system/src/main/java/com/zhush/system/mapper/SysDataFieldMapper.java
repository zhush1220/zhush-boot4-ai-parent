package com.zhush.system.mapper;

import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import com.zhush.system.entity.SysDataField;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 受控字段元数据 Mapper
 *
 * @author zhush
 */
@Mapper
public interface SysDataFieldMapper extends BaseMapperX<SysDataField> {

    /**
     * 按资源ID查询受控字段
     */
    default List<SysDataField> selectListByResourceId(Long resourceId) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysDataField::getResourceId).eq(resourceId);
        queryWrapper.orderBy("field_path", true);
        return selectListByQuery(queryWrapper);
    }

    /**
     * 按权限标识查询字段（元数据同步幂等判断用）
     */
    default SysDataField selectByPermissionCode(String permissionCode) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysDataField::getPermissionCode).eq(permissionCode);
        return selectOneByQuery(queryWrapper);
    }

    /**
     * 按资源ID删除字段元数据
     */
    default void deleteByResourceId(Long resourceId) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysDataField::getResourceId).eq(resourceId);
        deleteByQuery(queryWrapper);
    }
}
