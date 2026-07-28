package com.zhush.system.security;

import cn.hutool.core.util.StrUtil;
import com.zhush.data.permission.core.DataScope;
import com.zhush.data.permission.core.DataScopeProvider;
import com.zhush.data.permission.core.DataScopeType;
import com.zhush.satoken.core.StpAdminUtil;
import com.zhush.system.entity.SysAdminUser;
import com.zhush.system.entity.SysRole;
import com.zhush.system.mapper.SysAdminUserMapper;
import com.zhush.system.service.AdminDeptService;
import com.zhush.system.service.AdminPermissionService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.zhush.system.service.AdminPermissionService.SUPER_ADMIN_CODE;

/**
 * 管理端数据范围提供者
 * <p>
 * 根据当前登录管理员的角色配置（{@code sys_role.data_scope}）解析可见数据范围：
 * <ul>
 *     <li>未登录 / 超级管理员 / 任一角色为"全部"→ 不过滤</li>
 *     <li>多角色取并集：2-本部门、3-本部门及以下、5-自定义 → 汇总部门ID集合</li>
 *     <li>全部角色均为 4-仅本人 → 仅本人数据</li>
 * </ul>
 * 每条走 QueryWrapper 的 SQL 都会触发解析，结果按用户缓存 60 秒（角色数据范围调整后最迟 1 分钟生效）。
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

    private final AdminPermissionService permissionService;
    private final AdminDeptService deptService;
    private final SysAdminUserMapper adminUserMapper;

    private final Map<Long, CacheEntry> cache = new ConcurrentHashMap<>();

    public AdminDataScopeProvider(AdminPermissionService permissionService,
                                  AdminDeptService deptService,
                                  SysAdminUserMapper adminUserMapper) {
        this.permissionService = permissionService;
        this.deptService = deptService;
        this.adminUserMapper = adminUserMapper;
    }

    @Override
    public DataScope getDataScope() {
        Long userId = StpAdminUtil.getLoginIdDefaultNull();
        // 未登录（如定时任务、系统内部调用）不做行级过滤
        if (userId == null) {
            return DataScope.all();
        }
        long now = System.currentTimeMillis();
        CacheEntry entry = cache.get(userId);
        if (entry != null && entry.expireAt > now) {
            return entry.scope;
        }
        DataScope scope = resolve(userId);
        cache.put(userId, new CacheEntry(scope, now + CACHE_TTL_MILLIS));
        return scope;
    }

    // ================ 内部方法 ================

    private DataScope resolve(Long userId) {
        List<SysRole> roles = permissionService.listEnabledRoles(userId);
        // 无任何启用角色：按最小权限处理（仅本人）
        if (roles.isEmpty()) {
            return DataScope.self(userId);
        }
        // 超级管理员或任一角色为"全部"（未配置视为全部，兼容存量数据）
        boolean hasAll = roles.stream().anyMatch(r -> SUPER_ADMIN_CODE.equals(r.getCode())
                || r.getDataScope() == null || r.getDataScope() == SCOPE_ALL);
        if (hasAll) {
            return DataScope.all();
        }
        // 多角色取并集：汇总部门类范围的部门ID
        Set<Long> deptIds = new LinkedHashSet<>();
        boolean hasDeptScope = false;
        Long userDeptId = getUserDeptId(userId);
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

    private record CacheEntry(DataScope scope, long expireAt) {
    }

}
