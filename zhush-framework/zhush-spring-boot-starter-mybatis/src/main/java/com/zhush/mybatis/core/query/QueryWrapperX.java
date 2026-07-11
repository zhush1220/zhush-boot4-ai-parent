package com.zhush.mybatis.core.query;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.util.ArrayUtil;
import com.mybatisflex.core.util.LambdaGetter;
import com.mybatisflex.core.util.StringUtil;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Map;

/**
 * QueryWrapper 增强封装
 * 自动处理空值条件，避免空值参与查询条件构建
 * <p>
 * 支持的空值判断：
 * - null 值
 * - 空字符串 / 纯空白字符串
 * - 空 Collection
 * - 空 Map
 * - 空数组
 * @author: zhush
 * @date: 2026/06/27 10:40
 */
public class QueryWrapperX extends QueryWrapper {

    public static QueryWrapperX create() {
        return new QueryWrapperX();
    }

    public static QueryWrapperX create(@NonNull Object entity) {
        QueryWrapperX queryWrapperX = new QueryWrapperX();
        queryWrapperX.from(entity.getClass());
        return queryWrapperX;
    }

    // ==================== eq 等于 ====================

    @Override
    public QueryWrapperX eq(String column, Object value) {
        if (isNotEmpty(value)) {
            super.eq(column, value);
        }
        return this;
    }

    @Override
    public <T> QueryWrapperX eq(LambdaGetter<T> column, Object value) {
        if (isNotEmpty(value)) {
            super.eq(column, value);
        }
        return this;
    }

    // ==================== ne 不等于 ====================

    @Override
    public QueryWrapperX ne(String column, Object value) {
        if (isNotEmpty(value)) {
            super.ne(column, value);
        }
        return this;
    }

    @Override
    public <T> QueryWrapperX ne(LambdaGetter<T> column, Object value) {
        if (isNotEmpty(value)) {
            super.ne(column, value);
        }
        return this;
    }

    // ==================== gt 大于 ====================

    @Override
    public QueryWrapperX gt(String column, Object value) {
        if (isNotEmpty(value)) {
            super.gt(column, value);
        }
        return this;
    }

    @Override
    public <T> QueryWrapperX gt(LambdaGetter<T> column, Object value) {
        if (isNotEmpty(value)) {
            super.gt(column, value);
        }
        return this;
    }

    // ==================== ge 大于等于 ====================

    @Override
    public QueryWrapperX ge(String column, Object value) {
        if (isNotEmpty(value)) {
            super.ge(column, value);
        }
        return this;
    }

    @Override
    public <T> QueryWrapperX ge(LambdaGetter<T> column, Object value) {
        if (isNotEmpty(value)) {
            super.ge(column, value);
        }
        return this;
    }

    // ==================== lt 小于 ====================

    @Override
    public QueryWrapperX lt(String column, Object value) {
        if (isNotEmpty(value)) {
            super.lt(column, value);
        }
        return this;
    }

    @Override
    public <T> QueryWrapperX lt(LambdaGetter<T> column, Object value) {
        if (isNotEmpty(value)) {
            super.lt(column, value);
        }
        return this;
    }

    // ==================== le 小于等于 ====================

    @Override
    public QueryWrapperX le(String column, Object value) {
        if (isNotEmpty(value)) {
            super.le(column, value);
        }
        return this;
    }

    @Override
    public <T> QueryWrapperX le(LambdaGetter<T> column, Object value) {
        if (isNotEmpty(value)) {
            super.le(column, value);
        }
        return this;
    }

    // ==================== like 模糊匹配 %value% ====================

    @Override
    public QueryWrapperX like(String column, Object value) {
        if (isNotEmpty(value)) {
            super.like(column, value);
        }
        return this;
    }

    @Override
    public <T> QueryWrapperX like(LambdaGetter<T> column, Object value) {
        if (isNotEmpty(value)) {
            super.like(column, value);
        }
        return this;
    }

    // ==================== likeLeft 左模糊匹配 %value ====================

    @Override
    public QueryWrapperX likeLeft(String column, Object value) {
        if (isNotEmpty(value)) {
            super.likeLeft(column, value);
        }
        return this;
    }

    @Override
    public <T> QueryWrapperX likeLeft(LambdaGetter<T> column, Object value) {
        if (isNotEmpty(value)) {
            super.likeLeft(column, value);
        }
        return this;
    }

    // ==================== likeRight 右模糊匹配 value% ====================

    @Override
    public QueryWrapperX likeRight(String column, Object value) {
        if (isNotEmpty(value)) {
            super.likeRight(column, value);
        }
        return this;
    }

    @Override
    public <T> QueryWrapperX likeRight(LambdaGetter<T> column, Object value) {
        if (isNotEmpty(value)) {
            super.likeRight(column, value);
        }
        return this;
    }

    // ==================== notLike 不匹配 %value% ====================

    @Override
    public QueryWrapperX notLike(String column, Object value) {
        if (isNotEmpty(value)) {
            super.notLike(column, value);
        }
        return this;
    }

    @Override
    public <T> QueryWrapperX notLike(LambdaGetter<T> column, Object value) {
        if (isNotEmpty(value)) {
            super.notLike(column, value);
        }
        return this;
    }

    // ==================== notLikeLeft 不匹配 %value ====================

    @Override
    public QueryWrapperX notLikeLeft(String column, Object value) {
        if (isNotEmpty(value)) {
            super.notLikeLeft(column, value);
        }
        return this;
    }

    @Override
    public <T> QueryWrapperX notLikeLeft(LambdaGetter<T> column, Object value) {
        if (isNotEmpty(value)) {
            super.notLikeLeft(column, value);
        }
        return this;
    }

    // ==================== notLikeRight 不匹配 value% ====================

    @Override
    public QueryWrapperX notLikeRight(String column, Object value) {
        if (isNotEmpty(value)) {
            super.notLikeRight(column, value);
        }
        return this;
    }

    @Override
    public <T> QueryWrapperX notLikeRight(LambdaGetter<T> column, Object value) {
        if (isNotEmpty(value)) {
            super.notLikeRight(column, value);
        }
        return this;
    }

    // ==================== in 在范围内 ====================

    @Override
    public QueryWrapperX in(String column, Object... values) {
        if (isNotEmpty(values)) {
            super.in(column, values);
        }
        return this;
    }

    @Override
    public <T> QueryWrapperX in(LambdaGetter<T> column, Object... values) {
        if (isNotEmpty(values)) {
            super.in(column, values);
        }
        return this;
    }

    @Override
    public QueryWrapperX in(String column, Collection<?> values) {
        if (isNotEmpty(values)) {
            super.in(column, values);
        }
        return this;
    }

    @Override
    public <T> QueryWrapperX in(LambdaGetter<T> column, Collection<?> values) {
        if (isNotEmpty(values)) {
            super.in(column, values);
        }
        return this;
    }

    // ==================== notIn 不在范围内 ====================

    @Override
    public QueryWrapperX notIn(String column, Object... values) {
        if (isNotEmpty(values)) {
            super.notIn(column, values);
        }
        return this;
    }

    @Override
    public <T> QueryWrapperX notIn(LambdaGetter<T> column, Object... values) {
        if (isNotEmpty(values)) {
            super.notIn(column, values);
        }
        return this;
    }

    @Override
    public QueryWrapperX notIn(String column, Collection<?> values) {
        if (isNotEmpty(values)) {
            super.notIn(column, values);
        }
        return this;
    }

    @Override
    public <T> QueryWrapperX notIn(LambdaGetter<T> column, Collection<?> values) {
        if (isNotEmpty(values)) {
            super.notIn(column, values);
        }
        return this;
    }

    // ==================== between 在区间内 ====================

    @Override
    public QueryWrapperX between(String column, Object start, Object end) {
        if (isNotEmpty(start) && isNotEmpty(end)) {
            super.between(column, start, end);
        }
        return this;
    }

    @Override
    public <T> QueryWrapperX between(LambdaGetter<T> column, Object start, Object end) {
        if (isNotEmpty(start) && isNotEmpty(end)) {
            super.between(column, start, end);
        }
        return this;
    }

    // ==================== notBetween 不在区间内 ====================

    @Override
    public QueryWrapperX notBetween(String column, Object start, Object end) {
        if (isNotEmpty(start) && isNotEmpty(end)) {
            super.notBetween(column, start, end);
        }
        return this;
    }

    @Override
    public <T> QueryWrapperX notBetween(LambdaGetter<T> column, Object start, Object end) {
        if (isNotEmpty(start) && isNotEmpty(end)) {
            super.notBetween(column, start, end);
        }
        return this;
    }

    // ==================== 空值判断工具方法 ====================

    protected boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return StringUtil.noText((String) value);
        }
        if (value instanceof Collection<?>) {
            return ((Collection<?>) value).isEmpty();
        }
        if (value instanceof Map<?, ?>) {
            return ((Map<?, ?>) value).isEmpty();
        }
        if (value.getClass().isArray()) {
            return ArrayUtil.isEmpty((Object[]) value);
        }
        return false;
    }

    protected boolean isNotEmpty(Object value) {
        return !isEmpty(value);
    }
}
