package com.zhush.system.service;

import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.system.entity.SysRole;
import com.zhush.system.entity.SysRoleMenu;
import com.zhush.system.enums.SystemErrorCode;
import com.zhush.system.mapper.SysAdminUserRoleMapper;
import com.zhush.system.mapper.SysMenuMapper;
import com.zhush.system.mapper.SysRoleMapper;
import com.zhush.system.mapper.SysRoleMenuMapper;
import com.zhush.system.param.AdminRoleAssignDataScopeParam;
import com.zhush.system.param.AdminRoleParam;
import com.zhush.system.query.AdminRoleQuery;
import com.zhush.system.security.AdminDataScopeProvider;
import com.zhush.system.vo.AdminRoleVO;
import io.github.linpeilie.Converter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

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

    /**
     * 自定义数据范围（与 sys_role.data_scope 对应，其余：1-全部 2-本部门 3-本部门及以下 4-仅本人）
     */
    private static final int DATA_SCOPE_CUSTOM = 5;

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;
    private final SysAdminUserRoleMapper adminUserRoleMapper;
    private final Converter converter;
    /**
     * 数据范围缓存失效器：角色维度变更后主动失效，使调整对已登录用户立即生效，不必等缓存过期
     */
    private final AdminDataScopeProvider dataScopeProvider;

    public AdminRoleService(SysRoleMapper roleMapper,
                            SysRoleMenuMapper roleMenuMapper,
                            SysMenuMapper menuMapper,
                            SysAdminUserRoleMapper adminUserRoleMapper,
                            Converter converter,
                            AdminDataScopeProvider dataScopeProvider) {
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.menuMapper = menuMapper;
        this.adminUserRoleMapper = adminUserRoleMapper;
        this.converter = converter;
        this.dataScopeProvider = dataScopeProvider;
    }

    /**
     * 新增角色
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createRole(AdminRoleParam param) {
        validateCodeUnique(null, param.getCode());
        SysRole role = converter.convert(param, SysRole.class);
        if (role.getStatus() == null) {
            role.setStatus(STATUS_ENABLED);
        }
        roleMapper.insert(role);
        saveRoleMenus(role.getId(), param.getMenuIds());
        return role.getId();
    }

    /**
     * 修改角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(AdminRoleParam param) {
        SysRole existing = roleMapper.selectOneById(param.getId());
        assertNotNull(existing, SystemErrorCode.ROLE_NOT_FOUND, param.getId());

        SysRole update = converter.convert(param, SysRole.class);
        // 编码不可修改
        update.setCode(null);
        roleMapper.update(update);

        if (param.getMenuIds() != null) {
            roleMenuMapper.deleteByRoleId(param.getId());
            saveRoleMenus(param.getId(), param.getMenuIds());
        }
        // 角色名称/状态（禁用后不再参与数据范围计算）变更，全量失效数据范围缓存
        dataScopeProvider.invalidateAll();
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
        dataScopeProvider.invalidateAll();
    }

    /**
     * 批量删除角色（循环复用单条删除的校验与关联清理）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoleBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        ids.forEach(this::deleteRole);
    }

    /**
     * 角色详情
     */
    public AdminRoleVO getRole(Long id) {
        SysRole role = roleMapper.selectOneById(id);
        assertNotNull(role, SystemErrorCode.ROLE_NOT_FOUND, id);
        AdminRoleVO vo = converter.convert(role, AdminRoleVO.class);
        vo.setMenuIds(listMenuIdsByRoleId(id));
        return vo;
    }

    /**
     * 角色分页
     */
    public PageResult<AdminRoleVO> getRolePage(AdminRoleQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(SysRole::getName, query.getName())
                .like(SysRole::getCode, query.getCode())
                .eq(SysRole::getStatus, query.getStatus());
        queryWrapper.orderBy("sort", true);
        Page<SysRole> page = roleMapper.selectPageX(query, queryWrapper);
        List<AdminRoleVO> list = converter.convert(page.getRecords(), AdminRoleVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

    /**
     * 全部启用角色（下拉选择）
     */
    public List<AdminRoleVO> listAllEnabled() {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(SysRole::getStatus, STATUS_ENABLED);
        queryWrapper.orderBy("sort", true);
        return converter.convert(roleMapper.selectListByQuery(queryWrapper), AdminRoleVO.class);
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

    /**
     * 分配数据范围
     * <p>
     * 仅自定义（5）需要部门ID集合，其余类型清空；部门ID去 null 去重后以逗号分隔落库，
     * 由 {@link AdminDataScopeProvider} 解析时还原为集合。
     */
    public void assignDataScope(AdminRoleAssignDataScopeParam param) {
        SysRole existing = roleMapper.selectOneById(param.getRoleId());
        assertNotNull(existing, SystemErrorCode.ROLE_NOT_FOUND, param.getRoleId());

        List<Long> deptIds = CollUtil.isEmpty(param.getDataScopeDeptIds())
                ? List.of()
                : param.getDataScopeDeptIds().stream().filter(Objects::nonNull).distinct().toList();
        boolean custom = param.getDataScope() == DATA_SCOPE_CUSTOM;
        // 自定义必须选定部门：空集合会使该角色可见范围退化为"仅本人"，与操作者预期不符
        if (custom) {
            assertTrue(!deptIds.isEmpty(), SystemErrorCode.ROLE_DATA_SCOPE_DEPT_EMPTY);
        }

        SysRole update = new SysRole();
        update.setId(param.getRoleId());
        update.setDataScope(param.getDataScope());
        // 仅自定义保留部门ID串，其余类型清空以免残留旧配置影响后续切换
        update.setDataScopeDeptIds(custom && !deptIds.isEmpty() ? CollUtil.join(deptIds, ",") : "");
        roleMapper.update(update);
        // 数据范围变更需立即生效，避免 TTL 窗口期内仍按旧范围过滤
        dataScopeProvider.invalidateAll();
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
