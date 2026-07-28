package com.zhush.system.mapper;

import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import com.zhush.system.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 部门 Mapper
 *
 * @author zhush
 */
@Mapper
public interface SysDeptMapper extends BaseMapperX<SysDept> {

    /**
     * 按父部门ID查询子部门
     */
    default List<SysDept> selectListByParentId(Long parentId) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysDept::getParentId).eq(parentId);
        return selectListByQuery(queryWrapper);
    }
}
