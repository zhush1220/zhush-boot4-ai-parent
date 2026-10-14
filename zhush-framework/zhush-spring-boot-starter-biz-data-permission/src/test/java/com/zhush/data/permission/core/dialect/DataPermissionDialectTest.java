package com.zhush.data.permission.core.dialect;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.data.permission.annotation.DataPermission;
import com.zhush.data.permission.core.DataPermissionContextHolder;
import com.zhush.data.permission.core.DataScope;
import com.zhush.data.permission.core.DataScopeProvider;
import com.zhush.data.permission.core.DataScopeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 行级方言 prepareAuth 测试：资源感知路由、各数据范围条件生成、ALL 不注入、
 * 自定义条件叠加、以及"注入条件整体括号"的 OR 短路红线。
 *
 * @author zhush
 */
class DataPermissionDialectTest {

    /** 记录型 provider：区分默认解析与资源解析调用 */
    static class RecordingProvider implements DataScopeProvider {
        DataScope defaultScope = DataScope.all();
        final Map<String, DataScope> byResource = new HashMap<>();
        boolean defaultCalled;
        boolean resourceCalled;
        String lastResource;

        @Override
        public DataScope getDataScope() {
            defaultCalled = true;
            return defaultScope;
        }

        @Override
        public DataScope getDataScope(String resourceCode) {
            resourceCalled = true;
            lastResource = resourceCode;
            return byResource.getOrDefault(resourceCode, defaultScope);
        }
    }

    /** 注解夹具：获取真实 @DataPermission 注解实例 */
    static class Fixtures {
        @DataPermission(deptColumn = "dept_id", userColumn = "id")
        void global() {
        }

        @DataPermission(resource = "system:user", deptColumn = "dept_id", userColumn = "id")
        void resource() {
        }

        @DataPermission(enabled = false, deptColumn = "dept_id", userColumn = "id")
        void disabled() {
        }
    }

    private DataPermission annotation(String method) {
        try {
            return Fixtures.class.getDeclaredMethod(method).getAnnotation(DataPermission.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    private DataPermissionDialect dialect(RecordingProvider provider) {
        ObjectProvider<DataScopeProvider> objectProvider = new ObjectProvider<>() {
            @Override
            public DataScopeProvider getObject() {
                return provider;
            }

            @Override
            public DataScopeProvider getIfAvailable() {
                return provider;
            }
        };
        return new DataPermissionDialect(objectProvider);
    }

    @AfterEach
    void clearContext() {
        DataPermissionContextHolder.setIgnore(false);
        while (DataPermissionContextHolder.peek() != null) {
            DataPermissionContextHolder.pop();
        }
    }

    private String runPrepareAuth(DataPermission anno, RecordingProvider provider, QueryWrapper wrapper) {
        DataPermissionContextHolder.push(anno);
        try {
            dialect(provider).prepareAuth(wrapper, com.mybatisflex.core.dialect.OperateType.SELECT);
        } finally {
            DataPermissionContextHolder.pop();
        }
        // 去掉方言反引号包裹，便于跨断言稳定匹配列名（KeywordWrap.BACK_QUOTE）
        return wrapper.toSQL().replace("`", "");
    }

    @Test
    void deptScopeInjectsInCondition() {
        RecordingProvider provider = new RecordingProvider();
        provider.defaultScope = DataScope.dept(DataScopeType.DEPT, List.of(1L, 2L), 9L);

        String sql = runPrepareAuth(annotation("global"), provider, QueryWrapper.create());

        assertTrue(sql.contains("dept_id IN"), sql);
        // 未指定 resource 时走无参解析
        assertTrue(provider.defaultCalled);
        assertFalse(provider.resourceCalled);
    }

    @Test
    void resourcePresentRoutesToResourceAwareLookup() {
        RecordingProvider provider = new RecordingProvider();
        provider.byResource.put("system:user", DataScope.self(9L));

        String sql = runPrepareAuth(annotation("resource"), provider, QueryWrapper.create());

        assertTrue(provider.resourceCalled);
        assertFalse(provider.defaultCalled);
        assertTrue("system:user".equals(provider.lastResource));
        assertTrue(sql.contains("id ="), sql);
    }

    @Test
    void selfScopeInjectsUserColumnEq() {
        RecordingProvider provider = new RecordingProvider();
        provider.defaultScope = DataScope.self(9L);

        String sql = runPrepareAuth(annotation("global"), provider, QueryWrapper.create());

        assertTrue(sql.contains("id ="), sql);
    }

    @Test
    void allScopeWithoutExtraInjectsNothing() {
        RecordingProvider provider = new RecordingProvider();
        provider.defaultScope = DataScope.all();

        String sql = runPrepareAuth(annotation("global"), provider, QueryWrapper.create());

        assertFalse(sql.contains("dept_id"), sql);
        assertFalse(sql.contains("id ="), sql);
    }

    @Test
    void allScopeWithExtraConditionStillInjected() {
        RecordingProvider provider = new RecordingProvider();
        DataScope scope = DataScope.all();
        scope.addExtraCondition(new QueryColumn("status").eq(1));
        provider.defaultScope = scope;

        String sql = runPrepareAuth(annotation("global"), provider, QueryWrapper.create());

        assertTrue(sql.contains("status ="), sql);
    }

    @Test
    void disabledAnnotationSkipsFilter() {
        RecordingProvider provider = new RecordingProvider();
        provider.defaultScope = DataScope.dept(DataScopeType.DEPT, List.of(1L), 9L);

        String sql = runPrepareAuth(annotation("disabled"), provider, QueryWrapper.create());

        assertFalse(provider.defaultCalled);
        assertFalse(sql.contains("dept_id IN"), sql);
    }

    @Test
    void injectedConditionIsBracketedAgainstExistingOr() {
        RecordingProvider provider = new RecordingProvider();
        provider.defaultScope = DataScope.dept(DataScopeType.DEPT, List.of(1L, 2L), 9L);
        // 业务侧已有 OR 条件，注入条件必须整体括号后 AND，避免被短路（审计 R-1）
        QueryWrapper wrapper = QueryWrapper.create()
                .where(new QueryColumn("a").eq(1).or(new QueryColumn("b").eq(2)));

        String sql = runPrepareAuth(annotation("global"), provider, wrapper);

        // 数据范围条件被独立括号
        assertTrue(sql.contains("AND (") && sql.contains("dept_id IN"), sql);
    }
}
