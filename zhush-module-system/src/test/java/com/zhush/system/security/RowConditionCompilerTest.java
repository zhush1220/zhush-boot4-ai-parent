package com.zhush.system.security;

import com.mybatisflex.core.query.QueryCondition;
import com.mybatisflex.core.query.QueryWrapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 自定义行级条件编译器测试：白名单过滤（fail-closed）、操作符白名单、
 * $self 令牌替换、AND/OR 合并、以及保存侧校验拒绝非法配置。
 */
class RowConditionCompilerTest {

    private static final Set<String> ALLOWED = Set.of("dept_id", "id", "amount", "status");

    private String sql(List<QueryCondition> conditions) {
        QueryWrapper wrapper = QueryWrapper.create();
        conditions.forEach(wrapper::and);
        // 去反引号，稳定断言
        return wrapper.toSQL().replace("`", "");
    }

    @Test
    void compilesAndMergedWhitelistedConditions() {
        String json = "{\"logic\":\"AND\",\"conditions\":["
                + "{\"field\":\"amount\",\"op\":\">=\",\"value\":10000},"
                + "{\"field\":\"status\",\"op\":\"=\",\"value\":1}]}";

        List<QueryCondition> result = RowConditionCompiler.compile(json, ALLOWED, 42L);

        String sql = sql(result);
        assertTrue(sql.contains("amount >= 10000"), sql);
        assertTrue(sql.contains("status = 1"), sql);
        assertTrue(sql.contains(" AND "), sql);
    }

    @Test
    void compilesOrMergedConditions() {
        String json = "{\"logic\":\"OR\",\"conditions\":["
                + "{\"field\":\"status\",\"op\":\"=\",\"value\":1},"
                + "{\"field\":\"amount\",\"op\":\"<\",\"value\":100}]}";

        String sql = sql(RowConditionCompiler.compile(json, ALLOWED, 42L));

        assertTrue(sql.contains(" OR "), sql);
    }

    @Test
    void dropsIllegalFieldFailClosed() {
        String json = "{\"conditions\":[{\"field\":\"password\",\"op\":\"=\",\"value\":\"x\"}]}";

        List<QueryCondition> result = RowConditionCompiler.compile(json, ALLOWED, 42L);

        assertTrue(result.isEmpty());
    }

    @Test
    void dropsIllegalOperator() {
        String json = "{\"conditions\":[{\"field\":\"amount\",\"op\":\"; DROP TABLE\",\"value\":1}]}";

        List<QueryCondition> result = RowConditionCompiler.compile(json, ALLOWED, 42L);

        assertTrue(result.isEmpty());
    }

    @Test
    void selfTokenIsReplacedByUserId() {
        String json = "{\"conditions\":[{\"field\":\"id\",\"op\":\"=\",\"value\":\"$self\"}]}";

        String sql = sql(RowConditionCompiler.compile(json, ALLOWED, 42L));

        assertTrue(sql.contains("id = 42"), sql);
    }

    @Test
    void blankOrInvalidJsonYieldsNoCondition() {
        assertTrue(RowConditionCompiler.compile(null, ALLOWED, 1L).isEmpty());
        assertTrue(RowConditionCompiler.compile("not-a-json", ALLOWED, 1L).isEmpty());
    }

    @Test
    void validateAcceptsWhitelistedConfig() {
        String json = "{\"conditions\":[{\"field\":\"amount\",\"op\":\">=\",\"value\":10000}]}";

        assertTrue(RowConditionCompiler.validate(json, ALLOWED));
    }

    @Test
    void validateRejectsIllegalField() {
        String json = "{\"conditions\":[{\"field\":\"password\",\"op\":\"=\",\"value\":\"x\"}]}";

        assertThrows(IllegalArgumentException.class, () -> RowConditionCompiler.validate(json, ALLOWED));
    }

    @Test
    void validateRejectsIllegalOperator() {
        String json = "{\"conditions\":[{\"field\":\"amount\",\"op\":\"RAW\",\"value\":1}]}";

        assertThrows(IllegalArgumentException.class, () -> RowConditionCompiler.validate(json, ALLOWED));
    }

    @Test
    void validateRejectsEmptyConditions() {
        assertThrows(IllegalArgumentException.class,
                () -> RowConditionCompiler.validate("{\"conditions\":[]}", ALLOWED));
    }

    @Test
    void allowedColumnsAssemblesDeptUserAndFilters() {
        Set<String> columns = RowConditionCompiler.allowedColumns("dept_id", "id", "amount, status ,");

        assertEquals(Set.of("dept_id", "id", "amount", "status"), columns);
    }

}
