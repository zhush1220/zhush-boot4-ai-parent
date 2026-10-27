package com.zhush.agentscope.websearch;

import com.zhush.agentscope.config.AgentScopeProperties;
import com.zhush.agentscope.context.AgentContext;
import com.zhush.redis.core.RedisCache;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 联网门面行为测试（方案 6.3 / 6.5 / 6.7）：本轮开关、fail-open 降级、结果缓存与调用配额。
 *
 * <p>共同口径：任何异常都不允许打断对话，工具返回值必须是模型读得懂的 JSON 降级说明。
 */
class WebSearchServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 计数并可控返回结果的假供应商 */
    private static class StubProvider implements WebSearchProvider {

        private final AtomicInteger calls = new AtomicInteger();

        private RuntimeException failure;

        @Override
        public String code() {
            return "stub";
        }

        @Override
        public List<WebSearchResult> search(String query, int maxResults) throws IOException, InterruptedException {
            calls.incrementAndGet();
            if (failure != null) {
                throw failure;
            }
            return List.of(WebSearchResult.builder()
                    .title(query + " 的结果")
                    .url("https://example.com/a")
                    .snippet("摘要")
                    .source("example.com")
                    .build());
        }
    }

    private AgentScopeProperties properties() {
        AgentScopeProperties props = new AgentScopeProperties();
        props.getWebSearch().setEnabled(true);
        props.getWebSearch().setProvider("stub");
        return props;
    }

    private WebSearchService service(AgentScopeProperties props, StubProvider provider, RedisCache redisCache) {
        return new WebSearchService(props, List.of(provider), null, objectMapper, redisCache);
    }

    private AgentContext ctx(Boolean webSearchEnabled) {
        return AgentContext.builder()
                .sessionId("s-1").userId(7L).userType("member").webSearchEnabled(webSearchEnabled).build();
    }

    private JsonNode parse(String json) {
        return objectMapper.readTree(json);
    }

    @Test
    void turnWithoutWebSearchShouldNotReachProvider() {
        StubProvider provider = new StubProvider();
        WebSearchService service = service(properties(), provider, null);

        JsonNode result = parse(service.searchJson("天气", 5, ctx(false)));

        assertThat(result.path("ok").asBoolean()).isFalse();
        assertThat(result.path("reason").asString("")).isEqualTo("本轮未开启联网搜索");
        assertThat(result.path("hint").asString("")).isNotBlank();
        assertThat(provider.calls.get()).isZero();
    }

    @Test
    void sessionDefaultEnabledShouldApplyWhenTurnDoesNotDecide() {
        AgentScopeProperties props = properties();
        props.getWebSearch().setSessionDefaultEnabled(false);
        StubProvider provider = new StubProvider();

        JsonNode result = parse(service(props, provider, null).searchJson("天气", 5, ctx(null)));

        assertThat(result.path("reason").asString("")).isEqualTo("本轮未开启联网搜索");
        assertThat(provider.calls.get()).isZero();
    }

    @Test
    void providerFailureShouldDegradeInsteadOfThrowing() {
        StubProvider provider = new StubProvider();
        provider.failure = new RuntimeException("搜索服务返回 HTTP 429");
        WebSearchService service = service(properties(), provider, null);

        JsonNode result = parse(service.searchJson("天气", 5, ctx(true)));

        assertThat(result.path("ok").asBoolean()).isFalse();
        assertThat(result.path("reason").asString("")).contains("HTTP 429");
        assertThat(result.path("hint").asString("")).contains("未能联网");
    }

    @Test
    void ioExceptionShouldAlsoDegrade() {
        StubProvider provider = new StubProvider() {
            @Override
            public List<WebSearchResult> search(String query, int maxResults) throws IOException {
                throw new IOException("连接超时");
            }
        };

        JsonNode result = parse(service(properties(), provider, null).searchJson("天气", 5, ctx(true)));

        assertThat(result.path("ok").asBoolean()).isFalse();
        assertThat(result.path("reason").asString("")).contains("连接超时");
    }

    @Test
    void successfulSearchShouldReturnIndexedListWithSources() {
        WebSearchService service = service(properties(), new StubProvider(), null);

        JsonNode result = parse(service.searchJson("杭州天气", null, ctx(true)));

        assertThat(result.path("ok").asBoolean()).isTrue();
        assertThat(result.path("provider").asString("")).isEqualTo("stub");
        assertThat(result.path("cached").asBoolean()).isFalse();
        assertThat(result.path("count").asInt()).isEqualTo(1);
        JsonNode first = result.path("results").get(0);
        assertThat(first.path("index").asInt()).isEqualTo(1);
        assertThat(first.path("title").asString("")).isEqualTo("杭州天气 的结果");
        assertThat(first.path("url").asString("")).isEqualTo("https://example.com/a");
        assertThat(first.path("source").asString("")).isEqualTo("example.com");
        assertThat(result.path("hint").asString("")).contains("引用");
    }

    @Test
    void blankQueryShouldBeRefusedBeforeProvider() {
        StubProvider provider = new StubProvider();

        JsonNode result = parse(service(properties(), provider, null).searchJson("   ", 5, ctx(true)));

        assertThat(result.path("reason").asString("")).isEqualTo("查询词不能为空");
        assertThat(provider.calls.get()).isZero();
    }

    @Test
    void sameQueryShouldHitCacheWithoutSecondCall() {
        StubProvider provider = new StubProvider();
        WebSearchService service = service(properties(), provider, null);

        JsonNode first = parse(service.searchJson("杭州天气", 5, ctx(true)));
        JsonNode second = parse(service.searchJson(" 杭州天气 ", 5, ctx(true)));

        assertThat(first.path("cached").asBoolean()).isFalse();
        assertThat(second.path("cached").asBoolean()).isTrue();
        assertThat(provider.calls.get()).isEqualTo(1);
    }

    @Test
    void differentMaxResultsShouldNotShareCacheKey() {
        StubProvider provider = new StubProvider();

        WebSearchService service = service(properties(), provider, null);
        service.searchJson("杭州天气", 5, ctx(true));
        service.searchJson("杭州天气", 3, ctx(true));

        assertThat(provider.calls.get()).isEqualTo(2);
    }

    @Test
    void expiredCacheShouldCallProviderAgain() {
        AgentScopeProperties props = properties();
        props.getWebSearch().getSearch().setCacheTtl(java.time.Duration.ofMillis(-1));
        StubProvider provider = new StubProvider();

        WebSearchService service = service(props, provider, null);
        service.searchJson("杭州天气", 5, ctx(true));
        JsonNode second = parse(service.searchJson("杭州天气", 5, ctx(true)));

        assertThat(provider.calls.get()).isEqualTo(2);
        assertThat(second.path("cached").asBoolean()).isFalse();
    }

    @Test
    void sessionQuotaExceededShouldBlockProviderCall() {
        AgentScopeProperties props = properties();
        props.getWebSearch().getQuota().setCallsPerSession(2);
        RedisCache redisCache = mock(RedisCache.class);
        when(redisCache.increment(anyString(), anyLong())).thenReturn(3L);
        StubProvider provider = new StubProvider();

        JsonNode result = parse(service(props, provider, redisCache).searchJson("天气", 5, ctx(true)));

        assertThat(result.path("ok").asBoolean()).isFalse();
        assertThat(result.path("reason").asString("")).contains("本会话联网次数已达上限");
        assertThat(provider.calls.get()).isZero();
    }

    @Test
    void dailyQuotaExceededShouldBlockProviderCall() {
        AgentScopeProperties props = properties();
        props.getWebSearch().getQuota().setCallsPerDayPerUser(1);
        RedisCache redisCache = mock(RedisCache.class);
        // 会话键首次计数放行，日键已超限
        when(redisCache.increment(anyString(), anyLong())).thenReturn(1L, 2L);
        StubProvider provider = new StubProvider();

        JsonNode result = parse(service(props, provider, redisCache).searchJson("天气", 5, ctx(true)));

        assertThat(result.path("reason").asString("")).contains("今日联网额度已用完");
        assertThat(provider.calls.get()).isZero();
    }

    @Test
    void firstQuotaCallShouldSetTtlOnBothKeys() {
        AgentScopeProperties props = properties();
        props.getWebSearch().setSessionDefaultEnabled(true);
        RedisCache redisCache = mock(RedisCache.class);
        when(redisCache.increment(anyString(), anyLong())).thenReturn(1L);

        service(props, new StubProvider(), redisCache).searchJson("天气", 5, ctx(true));

        verify(redisCache).increment(org.mockito.ArgumentMatchers.startsWith("ai:websearch:cnt:s:"), anyLong());
        verify(redisCache).increment(org.mockito.ArgumentMatchers.startsWith("ai:websearch:cnt:d:member:7:"), anyLong());
        verify(redisCache, never()).increment(org.mockito.ArgumentMatchers.startsWith("ai:websearch:q:"), anyLong());
    }

    @Test
    void redisFailureShouldFailOpenRatherThanBlock() {
        RedisCache redisCache = mock(RedisCache.class);
        when(redisCache.increment(anyString(), anyLong())).thenThrow(new RuntimeException("redis down"));
        StubProvider provider = new StubProvider();

        JsonNode result = parse(service(properties(), provider, redisCache).searchJson("天气", 5, ctx(true)));

        assertThat(result.path("ok").asBoolean()).isTrue();
        assertThat(provider.calls.get()).isEqualTo(1);
    }

    @Test
    void missingProviderCodeShouldDegrade() {
        AgentScopeProperties props = properties();
        props.getWebSearch().setProvider("iqs");

        JsonNode result = parse(service(props, new StubProvider(), null).searchJson("天气", 5, ctx(true)));

        assertThat(result.path("ok").asBoolean()).isFalse();
        assertThat(result.path("reason").asString("")).contains("未配置可用的搜索供应商");
    }

    @Test
    void hasProviderShouldFollowConfiguredCode() {
        WebSearchService service = service(properties(), new StubProvider(), null);

        assertThat(service.hasProvider()).isTrue();
        AgentScopeProperties missing = properties();
        missing.getWebSearch().setProvider("iqs");
        assertThat(service(missing, new StubProvider(), null).hasProvider()).isFalse();
    }

    @Test
    void fetchGateShouldApplyBeforeFetcherAndReportDisabledCapability() {
        AgentScopeProperties props = properties();
        WebSearchService service = service(props, new StubProvider(), null);

        // pageFetcher 为 null：未被调用即证明闸门生效
        assertThat(parse(service.fetchJson("http://8.8.8.8/", 100, ctx(false)))
                .path("reason").asString("")).isEqualTo("本轮未开启联网搜索");
        props.getWebSearch().getFetch().setEnabled(false);
        assertThat(parse(service.fetchJson("http://8.8.8.8/", 100, ctx(true)))
                .path("reason").asString("")).isEqualTo("网页抓取能力未开启");
    }

    @Test
    void fetchFailureShouldDegradeWithReadableReason() throws Exception {
        AgentScopeProperties props = properties();
        WebPageFetcher pageFetcher = mock(WebPageFetcher.class);
        when(pageFetcher.fetch(any(), any())).thenThrow(new IOException("目标地址指向内网或保留地址，已拒绝抓取"));

        JsonNode result = parse(new WebSearchService(props, List.of(new StubProvider()),
                pageFetcher, objectMapper, null).fetchJson("http://127.0.0.1/", 100, ctx(true)));

        assertThat(result.path("ok").asBoolean()).isFalse();
        assertThat(result.path("reason").asString("")).contains("内网或保留地址");
    }

    @Test
    void fetchSuccessShouldMarkTextAsUntrusted() throws Exception {
        WebPageFetcher pageFetcher = mock(WebPageFetcher.class);
        when(pageFetcher.fetch(any(), any())).thenReturn(
                new WebPageFetcher.FetchedPage("https://example.com/a", 200, "text/html", "页面正文", false));

        JsonNode result = parse(new WebSearchService(properties(), List.of(new StubProvider()),
                pageFetcher, objectMapper, null).fetchJson("https://example.com/a", 100, ctx(true)));

        assertThat(result.path("ok").asBoolean()).isTrue();
        assertThat(result.path("text").asString("")).isEqualTo("页面正文");
        assertThat(result.path("truncated").asBoolean()).isFalse();
        assertThat(result.path("hint").asString("")).contains("不可信");
    }
}
