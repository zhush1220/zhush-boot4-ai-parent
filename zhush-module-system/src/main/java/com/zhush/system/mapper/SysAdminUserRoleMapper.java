package com.zhush.system.mapper;

import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import com.zhush.system.entity.SysAdminUserRole;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 管理员-角色关联 Mapper
 *
 * @author zhush
 */
@Mapper
public interface SysAdminUserRoleMapper extends BaseMapperX<SysAdminUserRole> {

    /**
     * 按用户ID查询关联
     */
    default List<SysAdminUserRole> selectListByUserId(Long userId) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysAdminUserRole::getUserId).eq(userId);
        return selectListByQuery(queryWrapper);
    }

    /**
     * 按用户ID集合查询关联
     */
    default List<SysAdminUserRole> selectListByUserIds(Collection<Long> userIds) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysAdminUserRole::getUserId).in(userIds);
        return selectListByQuery(queryWrapper);
    }

    /**
     * 按角色ID查询关联
     */
    default List<SysAdminUserRole> selectListByRoleId(Long roleId) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysAdminUserRole::getRoleId).eq(roleId);
        return selectListByQuery(queryWrapper);
    }

    /**
     * 按用户ID删除全部关联
     */
    default void deleteByUserId(Long userId) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysAdminUserRole::getUserId).eq(userId);
        deleteByQuery(queryWrapper);
    }
}
