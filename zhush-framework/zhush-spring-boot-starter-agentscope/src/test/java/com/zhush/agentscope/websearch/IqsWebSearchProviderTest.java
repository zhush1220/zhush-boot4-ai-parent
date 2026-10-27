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
 * IQS（阿里云信息查询服务）适配测试：端点、计费用请求字段与 {@code pageItems[]} 的字段映射。
 *
 * <p>{@code contents} 里正文/摘要必须为 false——按条计费且挤占 token，要正文走 L2 抓取工具。
 */
class IqsWebSearchProviderTest {

    private static final String ENDPOINT = "https://cloud-iqs.aliyuncs.com/search/unified";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = mock(HttpClient.class);

    private final IqsWebSearchProvider provider =
            new IqsWebSearchProvider(config("sk-iqs"), objectMapper, httpClient);

    private static AgentScopeProperties.WebSearch config(String apiKey) {
        AgentScopeProperties.WebSearch config = new AgentScopeProperties.WebSearch();
        config.setApiKey(apiKey);
        return config;
    }

    @Test
    void shouldPostUnifiedSearchEndpointWithBearerKey() throws Exception {
        WebSearchProviderTestSupport.respond(httpClient, 200, "{\"requestId\":\"r-1\",\"pageItems\":[]}");

        List<WebSearchResult> results = provider.search("杭州天气", 5);

        HttpRequest sent = WebSearchProviderTestSupport.captured(httpClient);
        WebSearchProviderTestSupport.assertJsonPost(sent, ENDPOINT, "sk-iqs");
        JsonNode body = WebSearchProviderTestSupport.body(sent);
        assertThat(body.path("query").asString("")).isEqualTo("杭州天气");
        assertThat(body.path("engineType").asString("")).isEqualTo("LiteAdvanced");
        assertThat(body.path("advancedParams").path("numResults").asInt()).isEqualTo(5);
        assertThat(body.path("contents").path("mainText").asBoolean()).isFalse();
        assertThat(body.path("contents").path("markdownText").asBoolean()).isFalse();
        assertThat(body.path("contents").path("summary").asBoolean()).isFalse();
        assertThat(body.path("contents").path("rerankScore").asBoolean()).isTrue();
        assertThat(results).isEmpty();
    }

    @Test
    void shouldMapPageItemsAndFallbackSourceToHost() throws Exception {
        String json = """
                {"requestId":"r-2","pageItems":[
                  {"title":"杭州今晚有雷雨","link":"https://news.example.com/a","snippet":"  局部阵雨，局地大暴雨 ",
                   "publishedTime":"2026-10-22T20:00:00Z","hostname":"Example新闻","rerankScore":0.91},
                  {"title":"缺站点名","link":"https://B.Example.net/x/r","snippet":"摘要二"},
                  {"title":"无链接，应被跳过"}
                ]}""";
        WebSearchProviderTestSupport.respond(httpClient, 200, json);

        List<WebSearchResult> results = provider.search("杭州天气", 5);

        assertThat(results).hasSize(2);
        WebSearchResult first = results.get(0);
        assertThat(first.getTitle()).isEqualTo("杭州今晚有雷雨");
        assertThat(first.getUrl()).isEqualTo("https://news.example.com/a");
        // 供应商摘要带首尾空白，映射时统一 trim
        assertThat(first.getSnippet()).isEqualTo("局部阵雨，局地大暴雨");
        assertThat(first.getSource()).isEqualTo("Example新闻");
        assertThat(first.getPublishedAt()).isEqualTo("2026-10-22T20:00:00Z");
        // hostname 缺失时用 URL 主机名兜底，前端来源卡片才不会空
        assertThat(results.get(1).getSource()).isEqualTo("b.example.net");
        assertThat(results.get(1).getPublishedAt()).isEmpty();
    }

    @Test
    void shouldHonourBaseUrlOverrideWithoutDoubleSlash() throws Exception {
        WebSearchProviderTestSupport.respond(httpClient, 200, "{\"pageItems\":[]}");

        new IqsWebSearchProvider(overrideBase("https://iqs.internal.example/"), objectMapper, httpClient)
                .search("q", 3);

        WebSearchProviderTestSupport.assertJsonPost(WebSearchProviderTestSupport.captured(httpClient),
                "https://iqs.internal.example/search/unified", "k");
    }

    @Test
    void nonSuccessStatusShouldBecomeReadableIoException() throws Exception {
        WebSearchProviderTestSupport.respond(httpClient, 429, "{\"code\":\"Throttling\",\"message\":\"超出配额\"}");

        assertThatThrownBy(() -> new IqsWebSearchProvider(config("k"), objectMapper, httpClient).search("q", 3))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("搜索服务返回 HTTP 429")
                .hasMessageContaining("Throttling");
    }

    private static AgentScopeProperties.WebSearch overrideBase(String baseUrl) {
        AgentScopeProperties.WebSearch config = config("k");
        config.setBaseUrl(baseUrl);
        return config;
    }
}
