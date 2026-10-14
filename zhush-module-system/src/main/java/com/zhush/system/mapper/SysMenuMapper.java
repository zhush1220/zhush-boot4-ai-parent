package com.zhush.system.mapper;

import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import com.zhush.system.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 菜单 / 权限 Mapper
 *
 * @author zhush
 */
@Mapper
public interface SysMenuMapper extends BaseMapperX<SysMenu> {

    /**
     * 按父菜单ID查询子菜单
     */
    default List<SysMenu> selectListByParentId(Long parentId) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysMenu::getParentId).eq(parentId);
        return selectListByQuery(queryWrapper);
    }

    /**
     * 按权限标识查询菜单（元数据同步幂等判断用）
     */
    default SysMenu selectByPermission(String permission) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysMenu::getPermission).eq(permission);
        return selectOneByQuery(queryWrapper);
    }
}
