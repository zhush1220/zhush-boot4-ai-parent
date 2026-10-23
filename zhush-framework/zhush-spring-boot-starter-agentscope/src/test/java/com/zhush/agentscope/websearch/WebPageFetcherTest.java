package com.zhush.agentscope.websearch;

import com.zhush.agentscope.config.AgentScopeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L2 抓取的边界测试：逐跳重过 SSRF 闸门、体积上限与正文抽取。
 *
 * <p>重定向由本类手工推进（{@code HttpClient} 配 {@code Redirect.NEVER}），所以
 * 「302 → 127.0.0.1」必须在第二跳被拒——只在首跳校验等于没校验。
 *
 * <p>注意：响应桩对象必须先构造再进 {@code when(...)}，在 stub 表达式里现场 mock 会触发
 * Mockito 的 UnfinishedStubbing。
 */
class WebPageFetcherTest {

    private final AgentScopeProperties.WebSearch config = new AgentScopeProperties.WebSearch();

    private final HttpClient httpClient = mock(HttpClient.class);

    private WebPageFetcher fetcher;

    /** 带类型的任意 matcher：让 {@code send} 的泛型推断落在 InputStream 上 */
    @SuppressWarnings("unchecked")
    private static HttpResponse.BodyHandler<InputStream> anyBody() {
        return any(HttpResponse.BodyHandler.class);
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<InputStream> stubResponse(int status, Map<String, List<String>> headers, String body) {
        HttpResponse<InputStream> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        // HttpHeaders.of 的过滤器是 BiPredicate（头名, 头值），这里全部放行
        when(response.headers()).thenReturn(HttpHeaders.of(headers, (name, value) -> true));
        when(response.body()).thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
        return response;
    }

    private void willReturn(HttpResponse<InputStream> response) throws Exception {
        when(httpClient.send(any(HttpRequest.class), anyBody())).thenReturn(response);
    }

    private void willReturn(HttpResponse<InputStream> first, HttpResponse<InputStream> second) throws Exception {
        when(httpClient.send(any(HttpRequest.class), anyBody())).thenReturn(first, second);
    }

    @BeforeEach
    void setUp() {
        fetcher = new WebPageFetcher(config, httpClient, new WebFetchGuard(config.getFetch()));
    }

    @Test
    void redirectToLoopbackMustBeRefusedOnSecondHop() throws Exception {
        willReturn(stubResponse(302, Map.of("location", List.of("http://127.0.0.1/")), ""));

        assertThatThrownBy(() -> fetcher.fetch("http://8.8.8.8/", null))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("内网或保留地址");
        // 第二跳在发请求前就被闸门拦下
        verify(httpClient, times(1)).send(any(HttpRequest.class), anyBody());
    }

    @Test
    void redirectChainShouldStopAtMaxRedirects() throws Exception {
        config.getFetch().setMaxRedirects(1);
        willReturn(stubResponse(302, Map.of("location", List.of("/next")), "x"));

        assertThatThrownBy(() -> fetcher.fetch("http://8.8.8.8/", null))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("重定向次数超过上限");
    }

    @Test
    void redirectShouldBeRefusedWhenFollowDisabled() throws Exception {
        config.getFetch().setFollowRedirects(false);
        willReturn(stubResponse(301, Map.of("location", List.of("https://1.1.1.2/")), ""));

        assertThatThrownBy(() -> fetcher.fetch("http://8.8.8.8/", null))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("已按配置拒绝跟随");
    }

    @Test
    void redirectWithoutLocationShouldFail() throws Exception {
        willReturn(stubResponse(302, Map.of(), ""));

        assertThatThrownBy(() -> fetcher.fetch("http://8.8.8.8/", null))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("缺少 Location 头");
    }

    @Test
    void httpErrorShouldFailWithStatus() throws Exception {
        willReturn(stubResponse(404, Map.of("content-type", List.of("text/html")), "not found"));

        assertThatThrownBy(() -> fetcher.fetch("http://8.8.8.8/", null))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("HTTP 404");
    }

    @Test
    void binaryContentTypeShouldBeRejected() throws Exception {
        willReturn(stubResponse(200, Map.of("content-type", List.of("application/pdf")), "%PDF-1.7"));

        assertThatThrownBy(() -> fetcher.fetch("http://8.8.8.8/a.pdf", null))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("不是可解析的文本类型");
    }

    @Test
    void shouldCapDownloadedBytes() throws Exception {
        config.getFetch().setMaxBytes(64);
        config.getFetch().setMaxChars(10_000);
        willReturn(stubResponse(200, Map.of("content-type", List.of("text/plain")), "a".repeat(5_000)));

        WebPageFetcher.FetchedPage page = fetcher.fetch("http://8.8.8.8/", null);

        assertThat(page.text()).hasSize(64);
        assertThat(page.httpStatus()).isEqualTo(200);
        assertThat(page.contentType()).isEqualTo("text/plain");
        assertThat(page.finalUrl()).isEqualTo("http://8.8.8.8/");
    }

    @Test
    void shouldTruncateBodyToCharLimitAndFlagIt() throws Exception {
        willReturn(stubResponse(200, Map.of("content-type", List.of("text/plain")), "abcdefghij"));

        WebPageFetcher.FetchedPage page = fetcher.fetch("http://8.8.8.8/", 4);

        assertThat(page.text()).isEqualTo("abcd");
        assertThat(page.truncated()).isTrue();
    }

    @Test
    void shouldStripScriptAndTagsFromHtml() throws Exception {
        config.getFetch().setMaxBytes(100_000);
        String html = "<html><head><style>b{color:red}</style></head><body>"
                + "<script>alert('xss')</script><h1>标题</h1><p>正文&nbsp;一段</p></body></html>";
        willReturn(stubResponse(200, Map.of("content-type", List.of("text/html;charset=utf-8")), html));

        WebPageFetcher.FetchedPage page = fetcher.fetch("http://8.8.8.8/", null);

        assertThat(page.text()).contains("标题").contains("正文 一段");
        assertThat(page.text()).doesNotContain("alert").doesNotContain("color:red").doesNotContain("<");
        assertThat(page.contentType()).isEqualTo("text/html");
    }

    @Test
    void shouldFollowSafeRedirectOnce() throws Exception {
        willReturn(stubResponse(302, Map.of("location", List.of("https://1.1.1.2/final")), ""),
                stubResponse(200, Map.of("content-type", List.of("text/plain")), "final body"));

        WebPageFetcher.FetchedPage page = fetcher.fetch("http://8.8.8.8/", null);

        assertThat(page.finalUrl()).isEqualTo("https://1.1.1.2/final");
        assertThat(page.text()).isEqualTo("final body");
        verify(httpClient, times(2)).send(any(HttpRequest.class), anyBody());
    }

    @Test
    void shouldRejectBeforeAnyRequestWhenFetchDisabled() throws Exception {
        config.getFetch().setEnabled(false);

        assertThatThrownBy(() -> fetcher.fetch("http://8.8.8.8/", null))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("未开启");
        verify(httpClient, never()).send(any(HttpRequest.class), anyBody());
    }
}
