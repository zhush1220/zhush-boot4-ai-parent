package com.zhush.system.service;

import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.system.controller.admin.vo.role.AdminRolePageReqVO;
import com.zhush.system.controller.admin.vo.role.AdminRoleRespVO;
import com.zhush.system.controller.admin.vo.role.AdminRoleSaveReqVO;
import com.zhush.system.controller.admin.vo.role.AdminRoleUpdateReqVO;
import com.zhush.system.entity.SysRole;
import com.zhush.system.entity.SysRoleMenu;
import com.zhush.system.enums.SystemErrorCode;
import com.zhush.system.mapper.SysAdminUserRoleMapper;
import com.zhush.system.mapper.SysMenuMapper;
import com.zhush.system.mapper.SysRoleMapper;
import com.zhush.system.mapper.SysRoleMenuMapper;
import io.github.linpeilie.Converter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.zhush.common.exception.ServiceExceptionUtil.assertFalse;
import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;
import static com.zhush.common.exception.ServiceExceptionUtil.assertTrue;

/**
 * 角色管理服务（CRUD + 菜单授权）
 *
 * @author zhush
 */
@Service
public class AdminRoleService {

    private static final int STATUS_ENABLED = 1;

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;
    private final SysAdminUserRoleMapper adminUserRoleMapper;
    private final Converter converter;

    public AdminRoleService(SysRoleMapper roleMapper,
                            SysRoleMenuMapper roleMenuMapper,
                            SysMenuMapper menuMapper,
                            SysAdminUserRoleMapper adminUserRoleMapper,
                            Converter converter) {
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.menuMapper = menuMapper;
        this.adminUserRoleMapper = adminUserRoleMapper;
        this.converter = converter;
    }

    /**
     * 新增角色
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createRole(AdminRoleSaveReqVO reqVO) {
        validateCodeUnique(null, reqVO.getCode());
        SysRole role = converter.convert(reqVO, SysRole.class);
        if (role.getStatus() == null) {
            role.setStatus(STATUS_ENABLED);
        }
        roleMapper.insert(role);
        saveRoleMenus(role.getId(), reqVO.getMenuIds());
        return role.getId();
    }

    /**
     * 修改角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(AdminRoleUpdateReqVO reqVO) {
        SysRole existing = roleMapper.selectOneById(reqVO.getId());
        assertNotNull(existing, SystemErrorCode.ROLE_NOT_FOUND, reqVO.getId());

        SysRole update = converter.convert(reqVO, SysRole.class);
        // 编码不可修改
        update.setCode(null);
        roleMapper.update(update);

        if (reqVO.getMenuIds() != null) {
            roleMenuMapper.deleteByRoleId(reqVO.getId());
            saveRoleMenus(reqVO.getId(), reqVO.getMenuIds());
        }
    }

    /**
     * 删除角色（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        SysRole existing = roleMapper.selectOneById(id);
        assertNotNull(existing, SystemErrorCode.ROLE_NOT_FOUND, id);
        // 存在用户绑定该角色时不可删除
        assertTrue(adminUserRoleMapper.selectListByRoleId(id).isEmpty(),
                SystemErrorCode.ROLE_HAS_USER);
        roleMapper.deleteById(id);
        roleMenuMapper.deleteByRoleId(id);
    }

    /**
     * 角色详情
     */
    public AdminRoleRespVO getRole(Long id) {
        SysRole role = roleMapper.selectOneById(id);
        assertNotNull(role, SystemErrorCode.ROLE_NOT_FOUND, id);
        AdminRoleRespVO vo = converter.convert(role, AdminRoleRespVO.class);
        vo.setMenuIds(listMenuIdsByRoleId(id));
        return vo;
    }

    /**
     * 角色分页
     */
    public PageResult<AdminRoleRespVO> getRolePage(AdminRolePageReqVO reqVO) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(SysRole::getName, reqVO.getName())
                .like(SysRole::getCode, reqVO.getCode())
                .eq(SysRole::getStatus, reqVO.getStatus());
        queryWrapper.orderBy("sort", true);
        Page<SysRole> page = roleMapper.selectPageX(reqVO, queryWrapper);
        List<AdminRoleRespVO> list = converter.convert(page.getRecords(), AdminRoleRespVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

    /**
     * 全部启用角色（下拉选择）
     */
    public List<AdminRoleRespVO> listAllEnabled() {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(SysRole::getStatus, STATUS_ENABLED);
        queryWrapper.orderBy("sort", true);
        return converter.convert(roleMapper.selectListByQuery(queryWrapper), AdminRoleRespVO.class);
    }

    /**
     * 分配菜单（权限）
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        SysRole existing = roleMapper.selectOneById(roleId);
        assertNotNull(existing, SystemErrorCode.ROLE_NOT_FOUND, roleId);
        roleMenuMapper.deleteByRoleId(roleId);
        saveRoleMenus(roleId, menuIds);
    }

    // ================ 内部方法 ================

    private void validateCodeUnique(Long id, String code) {
        SysRole role = roleMapper.selectByCode(code);
        if (role == null) {
            return;
        }
        assertFalse(id == null || !role.getId().equals(id),
                SystemErrorCode.ROLE_CODE_DUPLICATE, code);
    }

    private void saveRoleMenus(Long roleId, List<Long> menuIds) {
        if (CollUtil.isEmpty(menuIds)) {
            return;
        }
        List<Long> validMenuIds = menuMapper.selectListByIds(menuIds).stream()
                .map(com.zhush.system.entity.SysMenu::getId)
                .toList();
        List<SysRoleMenu> relations = validMenuIds.stream().map(menuId -> {
            SysRoleMenu relation = new SysRoleMenu();
            relation.setRoleId(roleId);
            relation.setMenuId(menuId);
            return relation;
        }).toList();
        if (!relations.isEmpty()) {
            roleMenuMapper.insertBatchWithDb(relations);
        }
    }

    private List<Long> listMenuIdsByRoleId(Long roleId) {
        return roleMenuMapper.selectListByRoleId(roleId).stream()
                .map(SysRoleMenu::getMenuId)
                .toList();
    }

}
