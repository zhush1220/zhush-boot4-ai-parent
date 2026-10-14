package com.zhush.system.security;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryCondition;
import com.zhush.data.permission.core.DataScope;
import com.zhush.data.permission.core.DataScopeProvider;
import com.zhush.data.permission.core.DataScopeType;
import com.zhush.satoken.core.StpAdminUtil;
import com.zhush.system.entity.SysAdminUser;
import com.zhush.system.entity.SysDataResource;
import com.zhush.system.entity.SysRole;
import com.zhush.system.entity.SysRoleDataResource;
import com.zhush.system.mapper.SysAdminUserMapper;
import com.zhush.system.mapper.SysDataResourceMapper;
import com.zhush.system.mapper.SysRoleDataResourceMapper;
import com.zhush.system.service.AdminDeptService;
import com.zhush.system.service.AdminPermissionService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.zhush.system.service.AdminPermissionService.SUPER_ADMIN_CODE;

/**
 * 管理端数据范围提供者
 * <p>
 * 根据当前登录管理员的角色配置解析可见数据范围：
 * <ul>
 *     <li>未登录 / 超级管理员 / 任一角色为"全部"→ 不过滤</li>
 *     <li>多角色取并集：2-本部门、3-本部门及以下、5-自定义 → 汇总部门ID集合</li>
 *     <li>全部角色均为 4-仅本人 → 仅本人数据</li>
 * </ul>
 * 在"角色全局范围"之上叠加"角色 × 资源"行级覆盖（{@code sys_role_data_resource}）：
 * {@link #getDataScope(String)} 命中资源覆盖规则时按覆盖规则解析，未配置则回落角色全局范围。
 * 解析结果按用户缓存 60 秒，规则/角色/部门变更后调用 {@link #invalidateAll()} / {@link #invalidate(Long)}
 * 主动失效，使调整立即生效。
 *
 * @author zhush
 */
@Component
public class AdminDataScopeProvider implements DataScopeProvider {

    /**
     * 数据范围类型（与 sys_role.data_scope 对应）
     */
    private static final int SCOPE_ALL = 1;
    private static final int SCOPE_DEPT = 2;
    private static final int SCOPE_DEPT_AND_CHILD = 3;
    private static final int SCOPE_SELF = 4;
    private static final int SCOPE_CUSTOM = 5;

    /**
     * 解析结果缓存有效期（毫秒）
     */
    private static final long CACHE_TTL_MILLIS = 60_000L;

    /**
     * 缓存条数达到该阈值时清理已过期条目，避免长期运行下离线用户条目无限堆积
     */
    private static final int CACHE_PURGE_THRESHOLD = 512;

    private final AdminPermissionService permissionService;
    private final AdminDeptService deptService;
    private final SysAdminUserMapper adminUserMapper;
    private final SysRoleDataResourceMapper roleDataResourceMapper;
    private final SysDataResourceMapper dataResourceMapper;

    private final Map<Long, CacheEntry> cache = new ConcurrentHashMap<>();

    public AdminDataScopeProvider(AdminPermissionService permissionService,
                                  AdminDeptService deptService,
                                  SysAdminUserMapper adminUserMapper,
                                  SysRoleDataResourceMapper roleDataResourceMapper,
                                  SysDataResourceMapper dataResourceMapper) {
        this.permissionService = permissionService;
        this.deptService = deptService;
        this.adminUserMapper = adminUserMapper;
        this.roleDataResourceMapper = roleDataResourceMapper;
        this.dataResourceMapper = dataResourceMapper;
    }

    @Override
    public DataScope getDataScope() {
        return snapshot().defaultScope();
    }

    @Override
    public DataScope getDataScope(String resourceCode) {
        if (StrUtil.isBlank(resourceCode)) {
            return getDataScope();
        }
        // 命中"角色 × 资源"覆盖规则用覆盖，否则回落角色全局范围
        Snapshot snapshot = snapshot();
        return snapshot.resourceScopes().getOrDefault(resourceCode, snapshot.defaultScope());
    }

    /**
     * 失效全部用户的数据范围缓存
     * <p>
     * 用于角色维度变更（数据范围调整、角色启停、菜单与角色关系、资源覆盖规则变化），
     * 无法廉价定位受影响用户，整体清空后下一次请求重新计算。
     */
    public void invalidateAll() {
        cache.clear();
    }

    /**
     * 失效指定用户的数据范围缓存（用户归属部门、所持角色变化时使用）
     */
    public void invalidate(Long userId) {
        if (userId != null) {
            cache.remove(userId);
        }
    }

    // ================ 内部方法 ================

    /**
     * 取得（并按需重建）当前用户的数据范围快照，未登录时返回不过滤快照
     */
    private Snapshot snapshot() {
        Long userId = StpAdminUtil.getLoginIdDefaultNull();
        // 未登录（如定时任务、系统内部调用）不做行级过滤
        if (userId == null) {
            return new Snapshot(DataScope.all(), Map.of());
        }
        long now = System.currentTimeMillis();
        CacheEntry entry = cache.get(userId);
        if (entry != null && entry.expireAt > now) {
            return entry.snapshot;
        }
        Snapshot snapshot = resolve(userId);
        purgeIfOversized(now);
        cache.put(userId, new CacheEntry(snapshot, now + CACHE_TTL_MILLIS));
        return snapshot;
    }

    /**
     * 缓存条目超阈值时顺带移除已过期条目
     */
    private void purgeIfOversized(long now) {
        if (cache.size() >= CACHE_PURGE_THRESHOLD) {
            cache.entrySet().removeIf(item -> item.getValue().expireAt() <= now);
        }
    }

    private Snapshot resolve(Long userId) {
        List<SysRole> roles = permissionService.listEnabledRoles(userId);
        // 无任何启用角色：按最小权限处理（仅本人），且不存在资源覆盖
        if (roles.isEmpty()) {
            return new Snapshot(DataScope.self(userId), Map.of());
        }
        List<Long> roleIds = roles.stream().map(SysRole::getId).toList();
        Long userDeptId = getUserDeptId(userId);
        DataScope defaultScope = computeDefaultScope(roles, userId, userDeptId);

        // 加载"角色 × 资源"覆盖规则并按资源分组解析
        List<SysRoleDataResource> rules = roleDataResourceMapper.selectListByRoleIds(roleIds);
        Map<String, DataScope> resourceScopes = Map.of();
        if (!rules.isEmpty()) {
            Map<String, List<SysRoleDataResource>> byResource = rules.stream()
                    .filter(r -> StrUtil.isNotBlank(r.getResourceCode()))
                    .collect(Collectors.groupingBy(SysRoleDataResource::getResourceCode));
            Map<String, DataScope> computed = new ConcurrentHashMap<>();
            byResource.forEach((code, list) ->
                    computed.put(code, computeResourceScope(list, userId, userDeptId, code)));
            resourceScopes = computed;
        }
        return new Snapshot(defaultScope, resourceScopes);
    }

    /**
     * 角色全局数据范围（多角色并集）
     */
    private DataScope computeDefaultScope(List<SysRole> roles, Long userId, Long userDeptId) {
        // 超级管理员或任一角色为"全部"（未配置视为全部，兼容存量数据）
        boolean hasAll = roles.stream().anyMatch(r -> SUPER_ADMIN_CODE.equals(r.getCode())
                || r.getDataScope() == null || r.getDataScope() == SCOPE_ALL);
        if (hasAll) {
            return DataScope.all();
        }
        Set<Long> deptIds = new LinkedHashSet<>();
        boolean hasDeptScope = false;
        for (SysRole role : roles) {
            switch (role.getDataScope()) {
                case SCOPE_DEPT -> {
                    hasDeptScope = true;
                    if (userDeptId != null) {
                        deptIds.add(userDeptId);
                    }
                }
                case SCOPE_DEPT_AND_CHILD -> {
                    hasDeptScope = true;
                    deptIds.addAll(deptService.getDeptAndChildIds(userDeptId));
                }
                case SCOPE_CUSTOM -> {
                    hasDeptScope = true;
                    deptIds.addAll(parseDeptIds(role.getDataScopeDeptIds()));
                }
                default -> {
                    // SCOPE_SELF：仅本人，无需收集部门
                }
            }
        }
        if (hasDeptScope) {
            // 部门集合为空时由框架兜底退化为"仅本人"
            return DataScope.dept(DataScopeType.CUSTOM, deptIds, userId);
        }
        return DataScope.self(userId);
    }

    /**
     * 单资源的覆盖规则解析（多角色命同一资源时并集；条件规则 AND 叠加为收窄）
     */
    private DataScope computeResourceScope(List<SysRoleDataResource> rules, Long userId,
                                           Long userDeptId, String resourceCode) {
        // 任一规则为"全部"→ 该资源不过滤（并集最宽）
        boolean hasAll = rules.stream()
                .anyMatch(r -> r.getScopeType() != null && r.getScopeType() == SysRoleDataResource.SCOPE_ALL);
        if (hasAll) {
            return DataScope.all();
        }
        Set<Long> deptIds = new LinkedHashSet<>();
        boolean hasDeptScope = false;
        boolean hasSelfScope = false;
        List<QueryCondition> conditions = new ArrayList<>();
        Set<String> allowedColumns = null;
        for (SysRoleDataResource rule : rules) {
            Integer scopeType = rule.getScopeType();
            if (scopeType == null) {
                continue;
            }
            switch (scopeType) {
                case SysRoleDataResource.SCOPE_DEPT -> {
                    hasDeptScope = true;
                    if (userDeptId != null) {
                        deptIds.add(userDeptId);
                    }
                }
                case SysRoleDataResource.SCOPE_DEPT_AND_CHILD -> {
                    hasDeptScope = true;
                    deptIds.addAll(deptService.getDeptAndChildIds(userDeptId));
                }
                case SysRoleDataResource.SCOPE_CUSTOM_DEPT -> {
                    hasDeptScope = true;
                    deptIds.addAll(parseDeptIds(rule.getDeptIds()));
                }
                case SysRoleDataResource.SCOPE_SELF -> hasSelfScope = true;
                case SysRoleDataResource.SCOPE_CONDITION -> {
                    // 自定义条件：加载资源列白名单后编译（$self 由后端注入用户ID）
                    if (allowedColumns == null) {
                        allowedColumns = resolveAllowedColumns(resourceCode);
                    }
                    conditions.addAll(RowConditionCompiler.compile(rule.getConditionJson(), allowedColumns, userId));
                }
                default -> {
                    // 未知类型忽略
                }
            }
        }
        // 组合：部门集合（CUSTOM 过滤）+ 条件（AND 收窄）；均无则仅本人兜底
        DataScope scope;
        if (hasDeptScope) {
            scope = DataScope.dept(DataScopeType.CUSTOM, deptIds, userId);
        } else if (!conditions.isEmpty()) {
            // 仅条件过滤：type=ALL 表示不加部门/本人基础条件，仅叠加编译后的条件
            scope = DataScope.all();
        } else if (hasSelfScope) {
            scope = DataScope.self(userId);
        } else {
            // 规则全为空/未知类型，回落到仅本人（fail-closed）
            scope = DataScope.self(userId);
        }
        conditions.forEach(scope::addExtraCondition);
        return scope;
    }

    /**
     * 解析资源的可引用列白名单（部门列 + 用户列 + 登记过滤列）
     */
    private Set<String> resolveAllowedColumns(String resourceCode) {
        SysDataResource resource = dataResourceMapper.selectByCode(resourceCode);
        if (resource == null) {
            return Set.of();
        }
        return RowConditionCompiler.allowedColumns(resource.getDeptColumn(),
                resource.getUserColumn(), resource.getFilterColumns());
    }

    private Long getUserDeptId(Long userId) {
        SysAdminUser user = adminUserMapper.selectOneById(userId);
        return user == null ? null : user.getDeptId();
    }

    private Set<Long> parseDeptIds(String dataScopeDeptIds) {
        Set<Long> result = new LinkedHashSet<>();
        if (StrUtil.isBlank(dataScopeDeptIds)) {
            return result;
        }
        for (long id : StrUtil.splitToLong(dataScopeDeptIds, ',')) {
            result.add(id);
        }
        return result;
    }

    /**
     * 用户数据范围快照：角色全局范围 + 按资源解析出的覆盖范围
     */
    private record Snapshot(DataScope defaultScope, Map<String, DataScope> resourceScopes) {
    }

    private record CacheEntry(Snapshot snapshot, long expireAt) {
    }

}
