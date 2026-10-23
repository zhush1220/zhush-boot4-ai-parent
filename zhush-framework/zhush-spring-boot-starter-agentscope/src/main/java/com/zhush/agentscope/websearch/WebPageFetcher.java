package com.zhush.agentscope.websearch;

import com.zhush.agentscope.config.AgentScopeProperties;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * L2 网页抓取：带 SSRF 闸门、体积上限与正文抽取的受限 HTTP GET。
 *
 * <p>不复用 Harness 内置 {@code web_fetch}（方案 2.1：无重定向跟随、无内网拦截、单次回传 20000 字符），
 * 也不逐跳信任 {@code followRedirects}——重定向由本类手工推进，每一跳都重新过
 * {@link WebFetchGuard}，否则一条 302 就能把校验绕过到内网。
 *
 * <p>不做浏览器渲染：JS 渲染站点取不到正文即明确失败（方案 3.2 的边界）。
 *
 * @author zhush
 */
public class WebPageFetcher {

    /** 单次读取的分块大小 */
    private static final int CHUNK_SIZE = 8192;

    /** 需要剥标签的 HTML 类内容类型 */
    private static final Pattern HTML_TYPE = Pattern.compile("(x?html|xml)$");

    /** 脚本/样式等与正文无关的块 */
    private static final Pattern NON_CONTENT_BLOCK =
            Pattern.compile("(?is)<(script|style|noscript|svg|template|head)[^>]*>.*?</\\1>");

    private static final Pattern TAG = Pattern.compile("(?s)<[^>]+>");

    private static final Pattern BLANK_LINES = Pattern.compile("\\n{3,}");

    private final AgentScopeProperties.WebSearch config;

    private final HttpClient httpClient;

    private final WebFetchGuard guard;

    public WebPageFetcher(AgentScopeProperties.WebSearch config, HttpClient httpClient, WebFetchGuard guard) {
        this.config = config;
        this.httpClient = httpClient;
        this.guard = guard;
    }

    /**
     * 抓取结果（正文已剥标签并截断）。
     *
     * @param finalUrl   跟随重定向后的最终地址
     * @param httpStatus 最终状态码
     * @param contentType 响应 Content-Type
     * @param text       正文纯文本
     * @param truncated  是否因上限被截断
     */
    public record FetchedPage(String finalUrl, int httpStatus, String contentType, String text, boolean truncated) {
    }

    /**
     * 抓取一个网页。
     *
     * @param url      起始地址
     * @param maxChars 正文字符上限（null 或非法值取配置默认）
     * @return 抓取结果
     * @throws IOException 地址被安全闸门拒绝、状态码异常、内容类型不支持或超时
     */
    public FetchedPage fetch(String url, Integer maxChars) throws IOException, InterruptedException {
        AgentScopeProperties.WebSearch.Fetch fetchConfig = config.getFetch();
        if (!fetchConfig.isEnabled()) {
            throw new IOException("网页抓取能力未开启");
        }
        int charLimit = clampChars(maxChars, fetchConfig.getMaxChars());
        String current = url;
        int hops = 0;
        while (true) {
            URI uri = guard.validate(current);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(fetchConfig.getTimeout() == null ? Duration.ofSeconds(10) : fetchConfig.getTimeout())
                    .header("Accept", "text/html,application/xhtml+xml,text/plain,application/json;q=0.9,*/*;q=0.1")
                    .header("User-Agent", userAgent())
                    .GET()
                    .build();
            HttpResponse<InputStream> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int status = response.statusCode();
            if (isRedirect(status)) {
                closeQuietly(response.body());
                if (!fetchConfig.isFollowRedirects()) {
                    throw new IOException("目标地址发生重定向，已按配置拒绝跟随");
                }
                if (hops >= fetchConfig.getMaxRedirects()) {
                    throw new IOException("重定向次数超过上限，已中止抓取");
                }
                String location = response.headers().firstValue("location").orElse(null);
                if (location == null || location.isBlank()) {
                    throw new IOException("重定向响应缺少 Location 头");
                }
                current = uri.resolve(location.strip()).toString();
                hops++;
                continue;
            }
            if (status >= 400) {
                closeQuietly(response.body());
                throw new IOException("目标站点返回 HTTP " + status);
            }
            String contentType = response.headers().firstValue("content-type").orElse("application/octet-stream");
            byte[] raw = readCapped(response.body(), fetchConfig.getMaxBytes());
            return extract(current, status, contentType, raw, charLimit);
        }
    }

    // ------------------------------------------------------------------
    // 内部
    // ------------------------------------------------------------------

    private FetchedPage extract(String finalUrl, int status, String contentType, byte[] raw, int charLimit)
            throws IOException {
        String base = contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
        boolean textual = base.startsWith("text/") || base.contains("html") || base.contains("xml")
                || base.contains("json");
        if (!textual) {
            throw new IOException("目标内容不是可解析的文本类型：" + base);
        }
        String text = decode(contentType, raw);
        if (base.contains("html") || base.contains("xml")) {
            text = toPlainText(text);
        }
        boolean truncated = text.length() > charLimit;
        if (truncated) {
            text = text.substring(0, charLimit);
        }
        return new FetchedPage(finalUrl, status, base, text, truncated);
    }

    /**
     * 按 Content-Type 声明的编码解码，缺失或非法时按 UTF-8。
     */
    private static String decode(String contentType, byte[] raw) {
        Charset charset = StandardCharsets.UTF_8;
        String lower = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf("charset=");
        if (idx >= 0) {
            String name = lower.substring(idx + "charset=".length()).split(";")[0].replace("\"", "").strip();
            try {
                charset = Charset.forName(name);
            } catch (RuntimeException e) {
                // 编码声明非法仍继续解析：回退 UTF-8 比抛错更有机会拿到正文
            }
        }
        return new String(raw, charset);
    }

    /**
     * 粗粒度正文抽取：去掉脚本/样式块与标签，还原常见实体，压缩空行。
     */
    static String toPlainText(String html) {
        String text = NON_CONTENT_BLOCK.matcher(html).replaceAll(" ");
        text = text.replaceAll("(?i)<br\\s*/?>", "\n");
        text = text.replaceAll("(?i)</(p|div|li|tr|h[1-6])>", "\n");
        text = TAG.matcher(text).replaceAll(" ");
        text = text.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<")
                .replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'");
        text = text.replaceAll("[ \\t\\x0B\\f\\r]+", " ");
        text = BLANK_LINES.matcher(text).replaceAll("\n\n");
        return text.strip();
    }

    /**
     * 带字节上限的读取：超出即停止，避免大文件/无限流打满堆内存。
     */
    private static byte[] readCapped(InputStream in, int maxBytes) throws IOException {
        try (InputStream stream = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(maxBytes, CHUNK_SIZE * 8));
            byte[] buffer = new byte[CHUNK_SIZE];
            int total = 0;
            int read;
            while (total < maxBytes && (read = stream.read(buffer)) != -1) {
                int keep = Math.min(read, maxBytes - total);
                out.write(buffer, 0, keep);
                total += keep;
            }
            return out.toByteArray();
        }
    }

    private static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    private static int clampChars(Integer maxChars, int configured) {
        if (maxChars == null || maxChars <= 0) {
            return configured;
        }
        return Math.min(maxChars, configured);
    }

    private static void closeQuietly(InputStream in) {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException ignored) {
            // 关闭失败不影响主流程
        }
    }

    /**
     * 可识别的 UA（部分站点按 UA 决定返回内容），固定串不含部署信息。
     */
    private static String userAgent() {
        return "Mozilla/5.0 (compatible; zhush-agent/1.0; +https://github.com)";
    }
}
