package com.zhush.system.mapper;

import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import com.zhush.system.entity.SysRoleDataResource;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 角色 × 资源行级覆盖规则 Mapper
 *
 * @author zhush
 */
@Mapper
public interface SysRoleDataResourceMapper extends BaseMapperX<SysRoleDataResource> {

    /**
     * 按角色ID集合查询全部覆盖规则（数据范围解析一次性加载后内存分组）
     */
    default List<SysRoleDataResource> selectListByRoleIds(Collection<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysRoleDataResource::getRoleId).in(roleIds);
        return selectListByQuery(queryWrapper);
    }

    /**
     * 按角色ID + 资源编码查询覆盖规则
     */
    default List<SysRoleDataResource> selectListByRoleIdAndResource(Long roleId, String resourceCode) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysRoleDataResource::getRoleId).eq(roleId);
        queryWrapper.and(SysRoleDataResource::getResourceCode).eq(resourceCode);
        return selectListByQuery(queryWrapper);
    }

    /**
     * 按角色ID删除全部覆盖规则（删除角色时级联清理）
     */
    default void deleteByRoleId(Long roleId) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysRoleDataResource::getRoleId).eq(roleId);
        deleteByQuery(queryWrapper);
    }
}
