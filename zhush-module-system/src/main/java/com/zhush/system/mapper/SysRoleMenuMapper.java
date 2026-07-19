package com.zhush.system.mapper;

import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import com.zhush.system.entity.SysRoleMenu;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 角色-菜单关联 Mapper
 *
 * @author zhush
 */
@Mapper
public interface SysRoleMenuMapper extends BaseMapperX<SysRoleMenu> {

    /**
     * 按角色ID集合查询关联
     */
    default List<SysRoleMenu> selectListByRoleIds(Collection<Long> roleIds) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysRoleMenu::getRoleId).in(roleIds);
        return selectListByQuery(queryWrapper);
    }

    /**
     * 按角色ID查询关联
     */
    default List<SysRoleMenu> selectListByRoleId(Long roleId) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysRoleMenu::getRoleId).eq(roleId);
        return selectListByQuery(queryWrapper);
    }

    /**
     * 按角色ID删除全部关联
     */
    default void deleteByRoleId(Long roleId) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysRoleMenu::getRoleId).eq(roleId);
        deleteByQuery(queryWrapper);
    }

    /**
     * 按菜单ID查询关联
     */
    default List<SysRoleMenu> selectListByMenuId(Long menuId) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysRoleMenu::getMenuId).eq(menuId);
        return selectListByQuery(queryWrapper);
    }

    /**
     * 按菜单ID删除全部关联
     */
    default void deleteByMenuId(Long menuId) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysRoleMenu::getMenuId).eq(menuId);
        deleteByQuery(queryWrapper);
    }
}
