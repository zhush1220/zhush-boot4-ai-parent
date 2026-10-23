package com.zhush.agentscope.websearch;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 搜索供应商的 HTTP 骨架：POST JSON + Bearer 鉴权 + 状态码校验 + 响应解析。
 *
 * <p>两个适配器（IQS / Tavily）的请求机制完全一致、只有报文字段不同，故把出网细节收敛在此，
 * 超时与非 2xx 一律抛 {@link IOException}，由 {@link WebSearchService} fail-open。
 *
 * @author zhush
 */
final class WebSearchHttp {

    private static final int MAX_LOG_BODY = 512;

    private WebSearchHttp() {
    }

    /**
     * 发起一次 JSON POST。
     *
     * @param uri     端点
     * @param apiKey  Bearer 令牌（调用方已保证非空）
     * @param payload 请求体对象（由 ObjectMapper 序列化）
     * @param timeout 单请求超时
     * @return 响应 JSON 根节点
     */
    static JsonNode postJson(HttpClient client, ObjectMapper mapper, URI uri, String apiKey,
                             Object payload, Duration timeout) throws IOException, InterruptedException {
        String body = mapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(timeout == null ? Duration.ofSeconds(8) : timeout)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IOException("搜索服务返回 HTTP " + status + "：" + abbreviate(response.body()));
        }
        return mapper.readTree(response.body());
    }

    /**
     * 按候选字段名取第一个非空文本（供应商字段名不一致时使用，如 link/url）。
     */
    static String text(JsonNode node, String... names) {
        if (node == null) {
            return "";
        }
        for (String name : names) {
            String value = node.path(name).asString("");
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    /**
     * 错误信息里的响应体片段（截断，避免异常消息带全量正文）。
     */
    static String abbreviate(String body) {
        if (body == null || body.isBlank()) {
            return "(空响应)";
        }
        String flat = body.replaceAll("\\s+", " ").trim();
        return flat.length() > MAX_LOG_BODY ? flat.substring(0, MAX_LOG_BODY) + "…" : flat;
    }

    /**
     * 取 URL 主机名（小写），非法地址返回空串——站点名兜底与域名校验共用。
     */
    static String host(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            String host = URI.create(url).getHost();
            return host == null ? "" : host.toLowerCase();
        } catch (IllegalArgumentException e) {
            return "";
        }
    }
}
