package com.zhush.system.security;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 自定义行级条件编译器（白名单 JSON → 参数化 {@link QueryCondition}）
 * <p>
 * 安全约束（对齐方案"OR 短路红线 / 不做 SpEL"）：
 * <ul>
 *     <li>字段白名单：仅允许 {@code allowedColumns} 内的列，命中不了则整条丢弃（读取侧 fail-closed，
 *         保存侧应在 {@link #validate} 中直接拒绝）</li>
 *     <li>操作符白名单：{@code = != > >= < <= in "not in" like}，其余丢弃</li>
 *     <li>值参数化：一律经 {@link QueryColumn} 条件方法绑定为占位符参数，绝不拼接原始 SQL</li>
 *     <li>{@code $self} 令牌：运行期由后端替换为当前用户ID，前端不得传入真实ID</li>
 * </ul>
 * condition_json 结构：{@code {"logic":"AND","conditions":[{"field":"amount","op":">=","value":10000}]}}
 *
 * @author zhush
 */
public final class RowConditionCompiler {

    private static final Logger log = LoggerFactory.getLogger(RowConditionCompiler.class);

    /**
     * 当前用户ID令牌（值位出现 "$self" 时替换为登录用户ID）
     */
    public static final String SELF_TOKEN = "$self";

    /**
     * 操作符白名单（小写比较）
     */
    private static final Set<String> ALLOWED_OPS = Set.of(
            "=", "!=", ">", ">=", "<", "<=", "in", "not in", "like");

    private RowConditionCompiler() {
    }

    /**
     * 编译为条件集合（读取侧：非法字段/操作符静默丢弃，绝不回落为原始 SQL）
     *
     * @param conditionJson  已登记的自定义条件 JSON
     * @param allowedColumns 允许引用的列白名单
     * @param userId         当前登录用户ID（用于 $self 替换，可为 null）
     * @return 编译得到的条件列表（AND 关系），无有效条件时返回空列表
     */
    public static List<QueryCondition> compile(String conditionJson, Set<String> allowedColumns, Long userId) {
        List<QueryCondition> result = new ArrayList<>();
        JSONObject root = parse(conditionJson);
        if (root == null) {
            return result;
        }
        JSONArray conditions = root.getJSONArray("conditions");
        if (conditions == null || conditions.isEmpty()) {
            return result;
        }
        boolean orLogic = "OR".equalsIgnoreCase(root.getStr("logic", "AND"));
        QueryCondition merged = null;
        for (Object item : conditions) {
            if (!(item instanceof JSONObject cond)) {
                continue;
            }
            QueryCondition condition = compileOne(cond, allowedColumns, userId);
            if (condition == null) {
                continue;
            }
            if (merged == null) {
                merged = condition;
            } else {
                merged = orLogic ? merged.or(condition) : merged.and(condition);
            }
        }
        if (merged != null) {
            result.add(merged);
        }
        return result;
    }

    /**
     * 保存侧校验：字段/操作符必须命中白名单，非法即抛异常（避免脏配置在读取侧被静默丢弃而与预期不符）
     *
     * @return 是否通过（不通过时抛 {@link IllegalArgumentException}）
     */
    public static boolean validate(String conditionJson, Set<String> allowedColumns) {
        JSONObject root = parse(conditionJson);
        if (root == null) {
            throw new IllegalArgumentException("自定义条件 JSON 非法");
        }
        JSONArray conditions = root.getJSONArray("conditions");
        if (conditions == null || conditions.isEmpty()) {
            throw new IllegalArgumentException("自定义条件不能为空");
        }
        for (Object item : conditions) {
            if (!(item instanceof JSONObject cond)) {
                throw new IllegalArgumentException("自定义条件项格式非法");
            }
            String field = cond.getStr("field");
            String op = normalizeOp(cond.getStr("op"));
            if (field == null || !allowedColumns.contains(field)) {
                throw new IllegalArgumentException("条件字段不在白名单：" + field);
            }
            if (op == null || !ALLOWED_OPS.contains(op)) {
                throw new IllegalArgumentException("条件操作符不在白名单：" + cond.getStr("op"));
            }
        }
        return true;
    }

    // ================ 内部方法 ================

    private static QueryCondition compileOne(JSONObject cond, Set<String> allowedColumns, Long userId) {
        String field = cond.getStr("field");
        String op = normalizeOp(cond.getStr("op"));
        if (field == null || !allowedColumns.contains(field)) {
            log.warn("[RowConditionCompiler] 丢弃非法条件字段：{}", field);
            return null;
        }
        if (op == null || !ALLOWED_OPS.contains(op)) {
            log.warn("[RowConditionCompiler] 丢弃非法条件操作符：{}", cond.getStr("op"));
            return null;
        }
        QueryColumn column = new QueryColumn(field);
        Object value = resolveValue(cond.get("value"), userId);
        return switch (op) {
            case "=" -> column.eq(value);
            case "!=" -> column.ne(value);
            case ">" -> column.gt(value);
            case ">=" -> column.ge(value);
            case "<" -> column.lt(value);
            case "<=" -> column.le(value);
            case "like" -> column.like(value);
            case "in" -> column.in(toCollection(value));
            case "not in" -> column.notIn(toCollection(value));
            default -> null;
        };
    }

    /**
     * $self 令牌替换为当前用户ID；其余原样返回（后续绑定为参数）
     */
    private static Object resolveValue(Object value, Long userId) {
        if (SELF_TOKEN.equals(value)) {
            return userId;
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private static Collection<?> toCollection(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection;
        }
        if (value instanceof Object[] array) {
            return List.of(array);
        }
        return List.of(value);
    }

    private static String normalizeOp(String op) {
        return op == null ? null : op.trim().toLowerCase();
    }

    private static JSONObject parse(String conditionJson) {
        if (conditionJson == null || conditionJson.isBlank()) {
            return null;
        }
        try {
            return JSONUtil.parseObj(conditionJson);
        } catch (RuntimeException ex) {
            log.warn("[RowConditionCompiler] 解析条件 JSON 失败：{}", ex.getMessage());
            return null;
        }
    }

    /**
     * 组装资源列白名单：部门列 + 用户列 + 登记的过滤列
     */
    public static Set<String> allowedColumns(String deptColumn, String userColumn, String filterColumns) {
        Set<String> columns = new LinkedHashSet<>();
        if (deptColumn != null && !deptColumn.isBlank()) {
            columns.add(deptColumn);
        }
        if (userColumn != null && !userColumn.isBlank()) {
            columns.add(userColumn);
        }
        if (filterColumns != null && !filterColumns.isBlank()) {
            for (String col : filterColumns.split(",")) {
                String trimmed = col.trim();
                if (!trimmed.isEmpty()) {
                    columns.add(trimmed);
                }
            }
        }
        return columns;
    }
}
