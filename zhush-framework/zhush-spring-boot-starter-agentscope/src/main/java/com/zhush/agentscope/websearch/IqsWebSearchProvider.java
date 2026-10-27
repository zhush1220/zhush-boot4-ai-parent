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
 * 阿里云信息查询服务 IQS（UnifiedSearch）适配层——生产默认供应商。
 *
 * <p>选择理由见方案 5.1：与既有阿里云账号/RAM 体系统一、国内出网时延可控、商用授权清晰。
 * 端点 {@code POST https://cloud-iqs.aliyuncs.com/search/unified}，Bearer 鉴权，
 * 结果在响应 {@code pageItems[]}（{@code title/link/snippet/publishedTime/hostname}）。
 * {@code contents} 只开 {@code rerankScore}：正文与增强摘要按条计费且会挤占 token 预算，
 * 需要正文时走 L2 抓取工具，不在搜索里顺带取回。
 *
 * @author zhush
 */
public class IqsWebSearchProvider implements WebSearchProvider {

    public static final String CODE = "iqs";

    private static final String DEFAULT_BASE_URL = "https://cloud-iqs.aliyuncs.com";

    private static final String SEARCH_PATH = "/search/unified";

    private final AgentScopeProperties.WebSearch config;

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient;

    private final URI endpoint;

    public IqsWebSearchProvider(AgentScopeProperties.WebSearch config, ObjectMapper objectMapper,
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
        Map<String, Object> contents = new LinkedHashMap<>();
        contents.put("mainText", false);
        contents.put("markdownText", false);
        contents.put("summary", false);
        contents.put("rerankScore", true);
        Map<String, Object> advancedParams = new LinkedHashMap<>();
        advancedParams.put("numResults", maxResults);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("query", query);
        payload.put("engineType", config.getEngineType());
        payload.put("contents", contents);
        payload.put("advancedParams", advancedParams);

        JsonNode root = WebSearchHttp.postJson(httpClient, objectMapper, endpoint, config.getApiKey(),
                payload, config.getSearch().getTimeout());
        List<WebSearchResult> results = new ArrayList<>();
        for (JsonNode item : root.path("pageItems")) {
            String url = WebSearchHttp.text(item, "link", "url");
            if (url.isEmpty()) {
                continue;
            }
            results.add(WebSearchResult.builder()
                    .title(WebSearchHttp.text(item, "title"))
                    .url(url)
                    .snippet(WebSearchHttp.text(item, "snippet", "summary", "description"))
                    .source(orHost(WebSearchHttp.text(item, "hostname"), url))
                    .publishedAt(WebSearchHttp.text(item, "publishedTime", "publishedDate"))
                    .build());
        }
        return results;
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    /**
     * 站点名缺失时用 URL 主机名兜底，保证前端来源卡片永远有可展示的归属。
     */
    private static String orHost(String hostName, String url) {
        return hostName.isEmpty() ? WebSearchHttp.host(url) : hostName;
    }
}
