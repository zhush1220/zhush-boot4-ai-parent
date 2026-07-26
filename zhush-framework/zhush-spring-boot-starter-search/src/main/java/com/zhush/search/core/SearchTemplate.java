package com.zhush.search.core;

import com.zhush.common.response.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;

import java.util.Collection;
import java.util.List;

/**
 * 全文搜索统一门面
 *
 * <p>封装索引管理、文档增删、关键字分页检索等常用操作，
 * 各业务模块统一通过本类访问 Elasticsearch，屏蔽底层 API 细节；
 * 复杂 DSL 场景可通过 {@link #getOperations()} 获取原生操作入口。
 *
 * <p>索引命名规范：{@code zhush_{module}_{business}}，如 {@code zhush_member_user}。
 *
 * @author zhush
 */
@RequiredArgsConstructor
public class SearchTemplate {

    private final ElasticsearchOperations operations;

    // ==================== 索引管理 ====================

    /**
     * 按文档类（@Document 注解）创建索引及映射，索引已存在则跳过
     *
     * @return true-新建成功 false-索引已存在
     */
    public boolean createIndex(Class<?> docClass) {
        IndexOperations indexOps = operations.indexOps(docClass);
        if (indexOps.exists()) {
            return false;
        }
        return indexOps.createWithMapping();
    }

    /** 删除文档类对应的索引 */
    public boolean deleteIndex(Class<?> docClass) {
        return operations.indexOps(docClass).delete();
    }

    /** 判断文档类对应的索引是否存在 */
    public boolean indexExists(Class<?> docClass) {
        return operations.indexOps(docClass).exists();
    }

    // ==================== 文档操作 ====================

    /** 保存/更新单个文档（按 @Id 覆盖写） */
    public <T> T save(T doc) {
        return operations.save(doc);
    }

    /** 批量保存/更新文档 */
    public <T> Iterable<T> saveBatch(Iterable<T> docs) {
        return operations.save(docs);
    }

    /** 按主键查询文档，不存在返回 null */
    public <T> T getById(String id, Class<T> docClass) {
        return operations.get(id, docClass);
    }

    /** 按主键删除文档，返回被删除文档的 id */
    public String deleteById(String id, Class<?> docClass) {
        return operations.delete(id, docClass);
    }

    // ==================== 检索 ====================

    /**
     * 关键字分页检索：对指定字段做分词匹配（OR 关系），按相关度排序
     *
     * @param keyword  搜索关键字
     * @param fields   参与匹配的字段名集合
     * @param pageNum  页码（从 1 开始）
     * @param pageSize 每页条数
     * @param docClass 文档类
     */
    public <T> PageResult<T> searchByKeyword(String keyword, Collection<String> fields,
                                             int pageNum, int pageSize, Class<T> docClass) {
        Criteria criteria = null;
        for (String field : fields) {
            criteria = criteria == null
                    ? new Criteria(field).matches(keyword)
                    : criteria.or(new Criteria(field).matches(keyword));
        }
        CriteriaQuery query = new CriteriaQuery(criteria, PageRequest.of(pageNum - 1, pageSize));
        return search(query, docClass);
    }

    /**
     * 通用分页检索：接收任意 {@link Query}（CriteriaQuery/NativeQuery/StringQuery）
     */
    public <T> PageResult<T> search(Query query, Class<T> docClass) {
        SearchHits<T> hits = operations.search(query, docClass);
        List<T> list = hits.getSearchHits().stream().map(SearchHit::getContent).toList();
        return PageResult.of(list, hits.getTotalHits());
    }

    /** 复杂场景直接使用原生 ElasticsearchOperations */
    public ElasticsearchOperations getOperations() {
        return operations;
    }
}
