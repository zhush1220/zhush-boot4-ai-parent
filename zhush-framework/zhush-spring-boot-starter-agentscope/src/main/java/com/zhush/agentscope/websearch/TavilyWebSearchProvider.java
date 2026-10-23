package com.zhush.agentscope.websearch;

import com.zhush.agentscope.config.AgentScopeProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tavily 适配层——开发期备选（Harness 内置工具即对接它，链路最易验证）。
 *
 * <p>注意免费版限个人/非商用且为海外服务，生产切换前先复核授权条款（方案 5.1、待确认决策 6）。
 * 端点 {@code POST https://api.tavily.com/search}，结果在响应 {@code results[]}。
 *
 * @author zhush
 */
public class TavilyWebSearchProvider implements WebSearchProvider {

    public static final String CODE = "tavily";

    private static final String DEFAULT_BASE_URL = "https://api.tavily.com";

    private static final String SEARCH_PATH = "/search";

    private final AgentScopeProperties.WebSearch config;

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient;

    private final URI endpoint;

    public TavilyWebSearchProvider(AgentScopeProperties.WebSearch config, ObjectMapper objectMapper,
                                   HttpClient httpClient) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        String base = config.getBaseUrl() == null || config.getBaseUrl().isBlank()
                ? DEFAULT_BASE_URL : trimTrailingSlash(config.getBaseUrl());
        this.endpoint = URI.create(base + SEARCH_PATH);
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<WebSearchResult> search(String query, int maxResults) throws IOException, InterruptedException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("query", query);
        payload.put("max_results", maxResults);
        payload.put("search_depth", "basic");
        payload.put("include_answer", false);
        payload.put("include_raw_content", false);

        JsonNode root = WebSearchHttp.postJson(httpClient, objectMapper, endpoint, config.getApiKey(),
                payload, config.getSearch().getTimeout());
        List<WebSearchResult> results = new ArrayList<>();
        for (JsonNode item : root.path("results")) {
            String url = WebSearchHttp.text(item, "url", "link");
            if (url.isEmpty()) {
                continue;
            }
            String host = WebSearchHttp.host(url);
            results.add(WebSearchResult.builder()
                    .title(WebSearchHttp.text(item, "title"))
                    .url(url)
                    .snippet(WebSearchHttp.text(item, "content", "snippet"))
                    .source(host.isEmpty() ? WebSearchHttp.text(item, "domain") : host)
                    .publishedAt(WebSearchHttp.text(item, "published_date"))
                    .build());
        }
        return results;
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
