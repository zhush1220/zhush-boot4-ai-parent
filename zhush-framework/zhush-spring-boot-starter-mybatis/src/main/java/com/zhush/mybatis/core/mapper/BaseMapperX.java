package com.zhush.mybatis.core.mapper;

import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.BatchArgsSetter;
import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.util.SqlUtil;
import com.zhush.common.query.BaseQuery;
import com.zhush.common.query.SortablePageQuery;
import com.zhush.common.query.SortingField;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 在 MyBatis Flex 的 BaseMapper 的基础上拓展，提供更多的能力
 */
public interface BaseMapperX<T> extends BaseMapper<T> {

    /**
     * 默认批量操作大小
     */
    int DEFAULT_BATCH_SIZE = 1000;

    /**
     * 分页查询
     * @param baseQuery 分页查询参数
     * @param queryWrapper 查询条件
     * @return 分页结果
     */
    default Page<T> selectPageX(BaseQuery baseQuery, QueryWrapper queryWrapper) {
        // 不分页：直接查询全部
        if (BaseQuery.PAGE_SIZE_NONE.equals(baseQuery.getPageSize())) {
            List<T> list = selectListByQuery(queryWrapper);
            Page<T> page = new Page<>();
            page.setRecords(list);
            page.setTotalRow((long) list.size());
            page.setPageNumber(1);
            page.setPageSize((long) list.size());
            return page;
        }

        // 正常分页
        Page<T> page = new Page<>(baseQuery.getPageNum(), baseQuery.getPageSize());
        return paginate(page, queryWrapper);
    }

    /**
     * 分页查询 + 排序
     * @param sortablePageQuery 分页查询参数
     * @param queryWrapper 查询条件
     * @return 分页结果
     */
    default Page<T> selectPageX(SortablePageQuery sortablePageQuery, QueryWrapper queryWrapper) {
        // 添加排序
        if (CollUtil.isNotEmpty(sortablePageQuery.getSortingFields())) {
            for (SortingField sortingField : sortablePageQuery.getSortingFields()) {
                if (sortingField.isAsc()) {
                    queryWrapper.orderBy(sortingField.getField(), true);
                } else {
                    queryWrapper.orderBy(sortingField.getField(), false);
                }
            }
        }
        return selectPageX((BaseQuery) sortablePageQuery, queryWrapper);
    }

    // ===================== 批量操作 =====================

    /**
     * 获取当前 Mapper 的 Class（解决泛型擦除导致的转型问题）
     * <p>
     * this.getClass() 返回 Class&lt;? extends BaseMapperX&gt;，
     * 无法直接转型为 Class&lt;BaseMapper&lt;T&gt;&gt;，通过 Class&lt;?&gt; 中间转型绕过。
     *
     * @return 当前 Mapper 接口 Class
     */
    @SuppressWarnings("unchecked")
    private Class<BaseMapper<T>> selfMapperClass() {
        return (Class<BaseMapper<T>>) (Class<?>) this.getClass();
    }

    /**
     * 批量插入（使用默认批量大小）
     *
     * @param entities 实体集合
     * @return 是否成功
     */
    default boolean insertBatchWithDb(Collection<T> entities) {
        return insertBatchWithDb(entities, DEFAULT_BATCH_SIZE);
    }

    /**
     * 批量插入（指定批量大小）
     *
     * @param entities  实体集合
     * @param batchSize 批量大小（&le; 0 时使用默认值）
     * @return 是否成功
     */
    default boolean insertBatchWithDb(Collection<T> entities, int batchSize) {
        if (CollUtil.isEmpty(entities)) {
            return false;
        }
        if (batchSize <= 0) {
            batchSize = DEFAULT_BATCH_SIZE;
        }
        int[] results = Db.executeBatch(entities, batchSize, selfMapperClass(), BaseMapper::insert);
        return SqlUtil.toBool(results);
    }

    /**
     * 批量插入（忽略 null 字段）
     *
     * @param entities 实体集合
     * @return 是否成功
     */
    default boolean insertBatchSelectiveWithDb(Collection<T> entities) {
        if (CollUtil.isEmpty(entities)) {
            return false;
        }
        int[] results = Db.executeBatch(entities, DEFAULT_BATCH_SIZE, selfMapperClass(), BaseMapper::insertSelective);
        return SqlUtil.toBool(results);
    }

    /**
     * 批量执行自定义 SQL（使用 Db.updateBatch）
     *
     * @param sql 自定义 SQL 语句
     * @param entities 实体集合
     * @param argsExtractor 参数提取器
     * @return 是否执行成功
     */
    default boolean executeBatch(String sql, Collection<T> entities,
                                 ArgsExtractor<T> argsExtractor) {
        if (CollUtil.isEmpty(entities)) {
            return false;
        }

        List<T> entityList = new ArrayList<>(entities);
        int[] results = Db.updateBatch(sql, new BatchArgsSetter() {
            @Override
            public int getBatchSize() {
                return entityList.size();
            }

            @Override
            public Object[] getSqlArgs(int index) {
                return argsExtractor.extract(entityList.get(index));
            }
        });
        return SqlUtil.toBool(results);
    }

    /**
     * 参数提取器函数式接口
     */
    @FunctionalInterface
    interface ArgsExtractor<T> {
        Object[] extract(T entity);
    }

    /**
     * 批量更新（使用默认批量大小）
     *
     * @param entities 实体集合
     * @return 受影响行数
     */
    default int updateBatchWithDb(Collection<T> entities) {
        return updateBatchWithDb(entities, DEFAULT_BATCH_SIZE);
    }

    /**
     * 批量更新（指定批量大小）
     *
     * @param entities  实体集合
     * @param batchSize 批量大小（&le; 0 时使用默认值）
     * @return 受影响行数
     */
    default int updateBatchWithDb(Collection<T> entities, int batchSize) {
        if (CollUtil.isEmpty(entities)) {
            return 0;
        }
        if (batchSize <= 0) {
            batchSize = DEFAULT_BATCH_SIZE;
        }
        return Db.updateEntitiesBatch(entities, batchSize);
    }

    /**
     * 批量更新（忽略 null 字段）
     *
     * @param entities 实体集合
     * @return 受影响行数
     */
    default int updateBatchSelectiveWithDb(Collection<T> entities) {
        if (CollUtil.isEmpty(entities)) {
            return 0;
        }
        return Db.updateEntitiesBatch(entities);
    }
}
