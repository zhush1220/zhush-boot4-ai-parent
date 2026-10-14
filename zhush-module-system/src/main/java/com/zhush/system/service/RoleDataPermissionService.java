package com.zhush.system.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.zhush.system.entity.SysDataField;
import com.zhush.system.entity.SysDataResource;
import com.zhush.system.entity.SysRole;
import com.zhush.system.entity.SysRoleDataResource;
import com.zhush.system.entity.SysRoleMenu;
import com.zhush.system.enums.SystemErrorCode;
import com.zhush.system.mapper.SysDataFieldMapper;
import com.zhush.system.mapper.SysDataResourceMapper;
import com.zhush.system.mapper.SysRoleDataResourceMapper;
import com.zhush.system.mapper.SysRoleMapper;
import com.zhush.system.mapper.SysRoleMenuMapper;
import com.zhush.system.param.RoleDataResourceSaveParam;
import com.zhush.system.param.RoleFieldPermissionParam;
import com.zhush.system.param.RoleResourceRuleItem;
import com.zhush.system.security.AdminDataScopeProvider;
import com.zhush.system.security.RowConditionCompiler;
import com.zhush.system.vo.DataFieldVO;
import com.zhush.system.vo.RoleDataPermissionVO;
import com.zhush.system.vo.RoleFieldPermissionVO;
import com.zhush.system.vo.RoleResourceRuleVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;
import static com.zhush.common.exception.ServiceExceptionUtil.assertTrue;
import static com.zhush.common.exception.ServiceExceptionUtil.exception;

/**
 * 角色数据权限服务（按业务对象的行级覆盖规则 + 列级字段授权）
 * <p>
 * 列级授权复用 {@code sys_role_menu}：字段的权限点即 sys_menu 按钮，授权/收回仅在该子集内增删，
 * 不改动角色其余菜单授权。
 *
 * @author zhush
 */
@Service
public class RoleDataPermissionService {

    /**
     * 自定义条件（scope_type=6）
     */
    private static final int SCOPE_CONDITION = SysRoleDataResource.SCOPE_CONDITION;
    /**
     * 自定义部门（scope_type=5）
     */
    private static final int SCOPE_CUSTOM_DEPT = SysRoleDataResource.SCOPE_CUSTOM_DEPT;

    private final SysRoleMapper roleMapper;
    private final SysRoleDataResourceMapper roleDataResourceMapper;
    private final SysDataResourceMapper dataResourceMapper;
    private final SysDataFieldMapper dataFieldMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final AdminDataScopeProvider dataScopeProvider;

    public RoleDataPermissionService(SysRoleMapper roleMapper,
                                     SysRoleDataResourceMapper roleDataResourceMapper,
                                     SysDataResourceMapper dataResourceMapper,
                                     SysDataFieldMapper dataFieldMapper,
                                     SysRoleMenuMapper roleMenuMapper,
                                     AdminDataScopeProvider dataScopeProvider) {
        this.roleMapper = roleMapper;
        this.roleDataResourceMapper = roleDataResourceMapper;
        this.dataResourceMapper = dataResourceMapper;
        this.dataFieldMapper = dataFieldMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.dataScopeProvider = dataScopeProvider;
    }

    /**
     * 查询角色的数据权限配置（默认范围 + 各资源行级覆盖）
     */
    public RoleDataPermissionVO getDataPermission(Long roleId) {
        SysRole role = roleMapper.selectOneById(roleId);
        assertNotNull(role, SystemErrorCode.ROLE_NOT_FOUND, roleId);

        RoleDataPermissionVO vo = new RoleDataPermissionVO();
        vo.setRoleId(roleId);
        vo.setDefaultScope(role.getDataScope());
        vo.setDefaultDeptIds(parseDeptIds(role.getDataScopeDeptIds()));

        // 以启用资源为基准，合并该角色已有覆盖（未覆盖资源 scopeType=null）
        Map<String, SysRoleDataResource> existing = loadRulesByResource(roleId);
        List<SysDataResource> resources = enabledResources();
        List<RoleResourceRuleVO> rules = new ArrayList<>();
        for (SysDataResource resource : resources) {
            if (resource.getRowEnabled() == null || resource.getRowEnabled() != 1) {
                continue;
            }
            RoleResourceRuleVO rule = new RoleResourceRuleVO();
            rule.setResourceCode(resource.getCode());
            rule.setResourceName(resource.getName());
            SysRoleDataResource saved = existing.get(resource.getCode());
            if (saved != null) {
                rule.setScopeType(saved.getScopeType());
                rule.setDeptIds(parseDeptIds(saved.getDeptIds()));
                rule.setConditionJson(saved.getConditionJson());
            }
            rules.add(rule);
        }
        vo.setRules(rules);
        return vo;
    }

    /**
     * 保存角色的行级覆盖规则（覆盖式：先清空该角色全部覆盖再落库）
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveRowRules(RoleDataResourceSaveParam param) {
        SysRole role = roleMapper.selectOneById(param.getRoleId());
        assertNotNull(role, SystemErrorCode.ROLE_NOT_FOUND, param.getRoleId());

        roleDataResourceMapper.deleteByRoleId(param.getRoleId());
        if (CollUtil.isEmpty(param.getRules())) {
            dataScopeProvider.invalidateAll();
            return;
        }
        List<SysRoleDataResource> toSave = new ArrayList<>();
        for (RoleResourceRuleItem item : param.getRules()) {
            if (item.getScopeType() == null) {
                // 未选择范围＝不覆盖，跳过
                continue;
            }
            SysDataResource resource = dataResourceMapper.selectByCode(item.getResourceCode());
            assertNotNull(resource, SystemErrorCode.DATA_RESOURCE_NOT_FOUND, item.getResourceCode());
            SysRoleDataResource rule = new SysRoleDataResource();
            rule.setRoleId(param.getRoleId());
            rule.setResourceCode(item.getResourceCode());
            rule.setScopeType(item.getScopeType());
            if (item.getScopeType() == SCOPE_CUSTOM_DEPT) {
                List<Long> deptIds = distinct(item.getDeptIds());
                assertTrue(!deptIds.isEmpty(), SystemErrorCode.ROLE_DATA_SCOPE_DEPT_EMPTY);
                rule.setDeptIds(CollUtil.join(deptIds, ","));
            } else {
                rule.setDeptIds("");
            }
            if (item.getScopeType() == SCOPE_CONDITION) {
                Set<String> allowed = RowConditionCompiler.allowedColumns(
                        resource.getDeptColumn(), resource.getUserColumn(), resource.getFilterColumns());
                try {
                    RowConditionCompiler.validate(item.getConditionJson(), allowed);
                } catch (IllegalArgumentException ex) {
                    throw exception(SystemErrorCode.ROLE_DATA_CONDITION_INVALID, ex, ex.getMessage());
                }
                rule.setConditionJson(item.getConditionJson());
            } else {
                rule.setConditionJson(null);
            }
            toSave.add(rule);
        }
        if (!toSave.isEmpty()) {
            roleDataResourceMapper.insertBatchWithDb(toSave);
        }
        // 行级覆盖变更需立即生效
        dataScopeProvider.invalidateAll();
    }

    /**
     * 保存角色列级字段授权（仅作用于字段权限菜单子集，保留其余菜单授权）
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveFieldPermissions(RoleFieldPermissionParam param) {
        SysRole role = roleMapper.selectOneById(param.getRoleId());
        assertNotNull(role, SystemErrorCode.ROLE_NOT_FOUND, param.getRoleId());

        Set<Long> fieldMenuIds = allFieldMenuIds();
        List<Long> grantIds = distinct(param.getMenuIds()).stream()
                .filter(fieldMenuIds::contains)
                .toList();

        // 移除角色当前的字段权限菜单授权，再按新授予回写
        List<SysRoleMenu> current = roleMenuMapper.selectListByRoleId(param.getRoleId());
        for (SysRoleMenu rm : current) {
            if (fieldMenuIds.contains(rm.getMenuId())) {
                roleMenuMapper.deleteById(rm.getId());
            }
        }
        List<SysRoleMenu> toInsert = grantIds.stream().map(menuId -> {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(param.getRoleId());
            rm.setMenuId(menuId);
            return rm;
        }).toList();
        if (!toInsert.isEmpty()) {
            roleMenuMapper.insertBatchWithDb(toInsert);
        }
    }

    /**
     * 列级字段授权视图：全部受控字段 + 角色已授权菜单ID集合
     */
    public RoleFieldPermissionVO getFieldPermission(Long roleId) {
        Set<Long> granted = roleMenuMapper.selectListByRoleId(roleId).stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        RoleFieldPermissionVO vo = new RoleFieldPermissionVO();
        vo.setRoleId(roleId);
        vo.setFields(allFields().stream().map(this::toFieldVO).toList());
        vo.setGrantedMenuIds(new ArrayList<>(granted));
        return vo;
    }

    // ================ 内部方法 ================

    private List<SysDataResource> enabledResources() {
        return dataResourceMapper.selectAllResources().stream()
                .filter(r -> r.getStatus() != null && r.getStatus() == 1)
                .toList();
    }

    private Map<String, SysRoleDataResource> loadRulesByResource(Long roleId) {
        return roleDataResourceMapper.selectListByRoleIds(List.of(roleId)).stream()
                .collect(Collectors.toMap(SysRoleDataResource::getResourceCode, r -> r, (a, b) -> a));
    }

    private List<SysDataField> allFields() {
        // 全部资源的受控字段（用于角色列级授权视图）
        List<SysDataField> result = new ArrayList<>();
        for (SysDataResource resource : enabledResources()) {
            if (resource.getFieldEnabled() != null && resource.getFieldEnabled() == 1) {
                result.addAll(dataFieldMapper.selectListByResourceId(resource.getId()));
            }
        }
        return result;
    }

    private Set<Long> allFieldMenuIds() {
        return allFields().stream()
                .map(SysDataField::getMenuId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<Long> distinct(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return List.of();
        }
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    private DataFieldVO toFieldVO(SysDataField field) {
        DataFieldVO vo = new DataFieldVO();
        vo.setId(field.getId());
        vo.setResourceId(field.getResourceId());
        vo.setFieldPath(field.getFieldPath());
        vo.setLabel(field.getLabel());
        vo.setPermissionCode(field.getPermissionCode());
        vo.setMenuId(field.getMenuId());
        vo.setDefaultStrategy(field.getDefaultStrategy());
        vo.setMaskValue(field.getMaskValue());
        vo.setStatus(field.getStatus());
        return vo;
    }

    private List<Long> parseDeptIds(String deptIds) {
        if (StrUtil.isBlank(deptIds)) {
            return List.of();
        }
        return java.util.Arrays.stream(deptIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .distinct()
                .toList();
    }
}
