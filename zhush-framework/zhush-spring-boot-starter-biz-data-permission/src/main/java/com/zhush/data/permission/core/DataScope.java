package com.zhush.data.permission.core;

import lombok.Data;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 当前用户的数据范围
 * <p>
 * 由业务侧 {@link DataScopeProvider} 解析产出：类型 + 归属用户ID + 可见部门ID集合。
 * 注意 DEPT_AND_CHILD 的子部门展开由业务侧完成，框架只消费最终的 deptIds。
 *
 * @author zhush
 */
@Data
public class DataScope {

    /**
     * 数据范围类型
     */
    private DataScopeType type;

    /**
     * 当前用户ID（SELF 或部门为空兜底时使用）
     */
    private Long userId;

    /**
     * 可见部门ID集合（DEPT / DEPT_AND_CHILD / CUSTOM 时使用，已含子部门展开结果）
     */
    private Set<Long> deptIds;

    /**
     * 全部数据（不过滤）
     */
    public static DataScope all() {
        DataScope scope = new DataScope();
        scope.setType(DataScopeType.ALL);
        return scope;
    }

    /**
     * 仅本人数据
     */
    public static DataScope self(Long userId) {
        DataScope scope = new DataScope();
        scope.setType(DataScopeType.SELF);
        scope.setUserId(userId);
        return scope;
    }

    /**
     * 部门范围数据（type 取 DEPT / DEPT_AND_CHILD / CUSTOM；userId 用于部门为空时兜底到仅本人）
     */
    public static DataScope dept(DataScopeType type, Collection<Long> deptIds, Long userId) {
        DataScope scope = new DataScope();
        scope.setType(type);
        scope.setDeptIds(deptIds == null ? Set.of() : new LinkedHashSet<>(deptIds));
        scope.setUserId(userId);
        return scope;
    }
}
