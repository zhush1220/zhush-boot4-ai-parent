package com.zhush.data.permission.core.dialect;

import com.mybatisflex.core.dialect.KeywordWrap;
import com.mybatisflex.core.dialect.LimitOffsetProcessor;
import com.mybatisflex.core.dialect.OperateType;
import com.mybatisflex.core.dialect.impl.CommonsDialectImpl;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryCondition;
import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.data.permission.annotation.DataPermission;
import com.zhush.data.permission.core.DataPermissionContextHolder;
import com.zhush.data.permission.core.DataScope;
import com.zhush.data.permission.core.DataScopeProvider;
import com.zhush.data.permission.core.DataScopeType;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 行级数据权限方言（MySQL）
 * <p>
 * 基于 MyBatis-Flex 官方数据权限扩展点 {@code prepareAuth}：当线程处于 @DataPermission
 * 上下文时，对 SELECT / UPDATE / DELETE 的 QueryWrapper 自动追加数据范围过滤条件。
 * 基于 Mapper XML 原生 SQL 或 byId 系列（无 QueryWrapper）的语句不会被过滤。
 * <p>
 * 当前按 MySQL 方言注册（反引号 + LIMIT 分页）；接入其他数据库时需按对应
 * KeywordWrap / LimitOffsetProcessor 另行注册。
 *
 * @author zhush
 */
public class DataPermissionDialect extends CommonsDialectImpl {

    /**
     * 数据范围提供者（懒获取，业务侧未注册实现时不做过滤）
     */
    private final ObjectProvider<DataScopeProvider> dataScopeProviders;

    public DataPermissionDialect(ObjectProvider<DataScopeProvider> dataScopeProviders) {
        super(KeywordWrap.BACK_QUOTE, LimitOffsetProcessor.MYSQL);
        this.dataScopeProviders = dataScopeProviders;
    }

    @Override
    public void prepareAuth(QueryWrapper queryWrapper, OperateType operateType) {
        if (operateType != OperateType.SELECT && operateType != OperateType.UPDATE
                && operateType != OperateType.DELETE) {
            return;
        }
        DataPermission annotation = DataPermissionContextHolder.peek();
        if (annotation == null || !annotation.enabled() || DataPermissionContextHolder.isIgnored()) {
            return;
        }
        DataScopeProvider provider = dataScopeProviders.getIfAvailable();
        if (provider == null) {
            return;
        }
        // 解析数据范围期间设置忽略标记，防止 provider 内部查库（查角色/部门）时递归触发过滤
        DataScope scope;
        DataPermissionContextHolder.setIgnore(true);
        try {
            scope = provider.getDataScope();
        } finally {
            DataPermissionContextHolder.setIgnore(false);
        }
        if (scope == null || scope.getType() == null || scope.getType() == DataScopeType.ALL) {
            return;
        }
        QueryCondition condition = buildCondition(annotation, scope);
        if (condition != null) {
            queryWrapper.and(condition);
        }
    }

    /**
     * 按数据范围类型构建过滤条件
     */
    private QueryCondition buildCondition(DataPermission annotation, DataScope scope) {
        return switch (scope.getType()) {
            case ALL -> null;
            case SELF -> selfCondition(annotation, scope);
            case DEPT, DEPT_AND_CHILD, CUSTOM -> deptCondition(annotation, scope);
        };
    }

    /**
     * 仅本人：userColumn = userId；用户ID缺失时兜底为不可见
     */
    private QueryCondition selfCondition(DataPermission annotation, DataScope scope) {
        if (scope.getUserId() == null) {
            return denyAll(annotation);
        }
        return new QueryColumn(annotation.userColumn()).eq(scope.getUserId());
    }

    /**
     * 部门范围：deptColumn IN (deptIds)；部门集合为空时退化为仅本人
     */
    private QueryCondition deptCondition(DataPermission annotation, DataScope scope) {
        if (scope.getDeptIds() == null || scope.getDeptIds().isEmpty()) {
            return selfCondition(annotation, scope);
        }
        return new QueryColumn(annotation.deptColumn()).in(scope.getDeptIds());
    }

    /**
     * 无法确定归属时的兜底条件（不返回任何数据，防止越权）
     */
    private QueryCondition denyAll(DataPermission annotation) {
        return new QueryColumn(annotation.userColumn()).eq(-1L);
    }
}
