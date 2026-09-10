package com.zhush.system.service;

import com.zhush.common.tree.TreeUtils;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.system.entity.SysDept;
import com.zhush.system.enums.SystemErrorCode;
import com.zhush.system.mapper.SysAdminUserMapper;
import com.zhush.system.mapper.SysDeptMapper;
import com.zhush.system.param.AdminDeptParam;
import com.zhush.system.query.AdminDeptQuery;
import com.zhush.system.vo.AdminDeptVO;
import io.github.linpeilie.Converter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;
import static com.zhush.common.exception.ServiceExceptionUtil.assertTrue;

/**
 * 部门管理服务（CRUD + 树形 + 子部门展开）
 *
 * @author zhush
 */
@Service
public class AdminDeptService {

    private static final long ROOT_PARENT_ID = 0L;
    private static final int STATUS_ENABLED = 1;

    private final SysDeptMapper deptMapper;
    private final SysAdminUserMapper adminUserMapper;
    private final Converter converter;

    public AdminDeptService(SysDeptMapper deptMapper,
                            SysAdminUserMapper adminUserMapper,
                            Converter converter) {
        this.deptMapper = deptMapper;
        this.adminUserMapper = adminUserMapper;
        this.converter = converter;
    }

    /**
     * 新增部门
     */
    public Long createDept(AdminDeptParam param) {
        if (param.getParentId() != null && param.getParentId() != ROOT_PARENT_ID) {
            assertNotNull(deptMapper.selectOneById(param.getParentId()),
                    SystemErrorCode.DEPT_NOT_FOUND, param.getParentId());
        }
        SysDept dept = converter.convert(param, SysDept.class);
        if (dept.getParentId() == null) {
            dept.setParentId(ROOT_PARENT_ID);
        }
        if (dept.getStatus() == null) {
            dept.setStatus(STATUS_ENABLED);
        }
        deptMapper.insert(dept);
        return dept.getId();
    }

    /**
     * 修改部门
     */
    public void updateDept(AdminDeptParam param) {
        SysDept existing = deptMapper.selectOneById(param.getId());
        assertNotNull(existing, SystemErrorCode.DEPT_NOT_FOUND, param.getId());
        // 父级不能设置为自身或其子孙部门（避免树成环）
        if (param.getParentId() != null && param.getParentId() != ROOT_PARENT_ID) {
            assertTrue(!getDeptAndChildIds(param.getId()).contains(param.getParentId()),
                    SystemErrorCode.DEPT_PARENT_ERROR);
        }
        SysDept update = converter.convert(param, SysDept.class);
        deptMapper.update(update);
    }

    /**
     * 删除部门（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDept(Long id) {
        SysDept existing = deptMapper.selectOneById(id);
        assertNotNull(existing, SystemErrorCode.DEPT_NOT_FOUND, id);
        // 存在子部门时不可删除
        assertTrue(deptMapper.selectListByParentId(id).isEmpty(),
                SystemErrorCode.DEPT_HAS_CHILDREN);
        // 部门下仍有用户时不可删除
        assertTrue(adminUserMapper.selectCountByDeptId(id) == 0,
                SystemErrorCode.DEPT_HAS_USER);
        deptMapper.deleteById(id);
    }

    /**
     * 批量删除部门（循环复用单条删除的校验与关联清理）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDeptBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        ids.forEach(this::deleteDept);
    }

    /**
     * 部门详情
     */
    public AdminDeptVO getDept(Long id) {
        SysDept dept = deptMapper.selectOneById(id);
        assertNotNull(dept, SystemErrorCode.DEPT_NOT_FOUND, id);
        return converter.convert(dept, AdminDeptVO.class);
    }

    /**
     * 部门列表（扁平，按过滤条件）
     */
    public List<AdminDeptVO> listDepts(AdminDeptQuery query) {
        return converter.convert(queryDepts(query), AdminDeptVO.class);
    }

    /**
     * 部门树
     */
    public List<AdminDeptVO> treeDepts(AdminDeptQuery query) {
        List<AdminDeptVO> list = converter.convert(queryDepts(query), AdminDeptVO.class);
        return TreeUtils.buildTrees(list);
    }

    /**
     * 获取指定部门及其全部子孙部门的ID集合（数据权限"本部门及以下"使用）
     */
    public Set<Long> getDeptAndChildIds(Long deptId) {
        Set<Long> result = new LinkedHashSet<>();
        if (deptId == null) {
            return result;
        }
        result.add(deptId);
        // 全量加载后内存按 parentId 分组展开，避免逐层查库
        List<SysDept> allDepts = deptMapper.selectAll();
        Map<Long, List<Long>> parentToChildren = new HashMap<>();
        for (SysDept dept : allDepts) {
            parentToChildren.computeIfAbsent(dept.getParentId(), k -> new ArrayList<>())
                    .add(dept.getId());
        }
        Deque<Long> queue = new ArrayDeque<>(List.of(deptId));
        while (!queue.isEmpty()) {
            List<Long> children = parentToChildren.get(queue.poll());
            if (children == null) {
                continue;
            }
            for (Long childId : children) {
                if (result.add(childId)) {
                    queue.offer(childId);
                }
            }
        }
        return result;
    }

    // ================ 内部方法 ================

    private List<SysDept> queryDepts(AdminDeptQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(SysDept::getName, query.getName())
                .eq(SysDept::getStatus, query.getStatus());
        queryWrapper.orderBy("sort", true);
        return deptMapper.selectListByQuery(queryWrapper);
    }

}
