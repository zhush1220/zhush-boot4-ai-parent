package com.zhush.search.config;

import com.zhush.search.core.SearchTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

/**
 * zhush-spring-boot-starter-search 自动装配入口
 *
 * <p>装配条件（全部满足才生效）：
 * <ul>
 *     <li>classpath 存在 Spring Data Elasticsearch</li>
 *     <li>显式配置 {@code zhush.search.enabled=true}（默认关闭，可自由启停）</li>
 * </ul>
 *
 * <p>装配内容：{@link SearchTemplate} 全文搜索统一门面。
 * ES 连接由 Spring Boot 官方自动配置负责（{@code spring.elasticsearch.uris} 等）。
 *
 * @author zhush
 */
@AutoConfiguration
@ConditionalOnClass(ElasticsearchOperations.class)
@ConditionalOnProperty(prefix = SearchProperties.PREFIX, name = "enabled", havingValue = "true")
@EnableConfigurationProperties(SearchProperties.class)
public class SearchAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ElasticsearchOperations.class)
    public SearchTemplate searchTemplate(ElasticsearchOperations elasticsearchOperations) {
        return new SearchTemplate(elasticsearchOperations);
    }
}
