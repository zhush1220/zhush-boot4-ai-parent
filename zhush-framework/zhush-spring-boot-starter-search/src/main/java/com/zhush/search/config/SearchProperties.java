package com.zhush.search.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 全文搜索配置项
 *
 * <p>默认关闭，需显式配置 {@code zhush.search.enabled=true} 才会装配搜索相关组件，
 * 未启用时不影响应用启动（无需部署 Elasticsearch）。
 *
 * @author zhush
 */
@Data
@ConfigurationProperties(prefix = SearchProperties.PREFIX)
public class SearchProperties {

    public static final String PREFIX = "zhush.search";

    /** 是否启用全文搜索（默认关闭） */
    private Boolean enabled = false;
}
