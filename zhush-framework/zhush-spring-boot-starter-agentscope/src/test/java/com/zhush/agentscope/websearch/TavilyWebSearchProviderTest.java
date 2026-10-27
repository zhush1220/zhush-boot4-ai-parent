package com.zhush.agentscope.websearch;

import com.zhush.agentscope.config.AgentScopeProperties;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Tavily 适配测试：端点、精简请求字段与 {@code results[]} 的字段映射。
 *
 * <p>{@code include_answer}/{@code include_raw_content} 关掉：答案由本项目模型生成，正文走 L2 抓取，
 * 不让供应商代答也带回整页原文。
 */
class TavilyWebSearchProviderTest {

    private static final String ENDPOINT = "https://api.tavily.com/search";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = mock(HttpClient.class);

    private final TavilyWebSearchProvider provider =
            new TavilyWebSearchProvider(config(), objectMapper, httpClient);

    private static AgentScopeProperties.WebSearch config() {
        AgentScopeProperties.WebSearch config = new AgentScopeProperties.WebSearch();
        config.setApiKey("tvly-test");
        return config;
    }

    @Test
    void shouldPostSearchEndpointWithBearerKey() throws Exception {
        WebSearchProviderTestSupport.respond(httpClient, 200, "{\"query\":\"q\",\"results\":[]}");

        List<WebSearchResult> results = provider.search("杭州天气", 3);

        HttpRequest sent = WebSearchProviderTestSupport.captured(httpClient);
        WebSearchProviderTestSupport.assertJsonPost(sent, ENDPOINT, "tvly-test");
        JsonNode body = WebSearchProviderTestSupport.body(sent);
        assertThat(body.path("query").asString("")).isEqualTo("杭州天气");
        assertThat(body.path("max_results").asInt()).isEqualTo(3);
        assertThat(body.path("search_depth").asString("")).isEqualTo("basic");
        assertThat(body.path("include_answer").asBoolean()).isFalse();
        assertThat(body.path("include_raw_content").asBoolean()).isFalse();
        assertThat(results).isEmpty();
    }

    @Test
    void shouldMapResultsWithHostAsSource() throws Exception {
        String json = """
                {"results":[
                  {"title":"暴雨预警","url":"https://www.weather.com.cn/alert","content":"今日局部阵雨",
                   "published_date":"2026-10-22","score":0.88},
                  {"title":"缺 url","content":"无链接，应被跳过"},
                  {"title":"仅域名可用","link":"https://news.example.org/a"}
                ]}""";
        WebSearchProviderTestSupport.respond(httpClient, 200, json);

        List<WebSearchResult> results = provider.search("杭州天气", 5);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getTitle()).isEqualTo("暴雨预警");
        assertThat(results.get(0).getUrl()).isEqualTo("https://www.weather.com.cn/alert");
        assertThat(results.get(0).getSnippet()).isEqualTo("今日局部阵雨");
        assertThat(results.get(0).getSource()).isEqualTo("www.weather.com.cn");
        assertThat(results.get(0).getPublishedAt()).isEqualTo("2026-10-22");
        assertThat(results.get(1).getUrl()).isEqualTo("https://news.example.org/a");
    }

    @Test
    void nonSuccessStatusShouldBecomeReadableIoException() throws Exception {
        WebSearchProviderTestSupport.respond(httpClient, 401, "{\"detail\":\"invalid api key\"}");

        assertThatThrownBy(() -> provider.search("q", 3))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("搜索服务返回 HTTP 401")
                .hasMessageContaining("invalid api key");
    }
}
