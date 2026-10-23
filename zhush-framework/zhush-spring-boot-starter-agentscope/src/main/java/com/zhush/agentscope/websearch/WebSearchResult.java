package com.zhush.agentscope.websearch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 单条搜索结果（L1 搜索的统一出参形态，供应商无关）。
 *
 * @author zhush
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSearchResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 标题 */
    private String title;

    /** 链接 */
    private String url;

    /** 摘要片段 */
    private String snippet;

    /** 站点名（缺失时由 url 主机名兜底） */
    private String source;

    /** 发布时间（供应商原样字符串，不做时区归一） */
    private String publishedAt;
}
