package com.zhush.ai.preview;

import com.zhush.ai.generation.RemoteFileFetcher;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * kkFileView 独立服务适配（路线甲-2，方案 §6.4）。
 *
 * <p><b>为什么下发 {@code server-iframe} 而不是 {@code server-pdf}</b>：kkFileView 对外只有
 * {@code /onlinePreview} 渲染页，没有「转换完把 PDF 字节给你」的稳定公开接口，而它的缓存目录与静态映射
 * 属内部实现，跟着小版本变。方案 S10 想要的「只取它当转换器、不取渲染页」在甲-1（LibreOffice 内嵌）上才成立；
 * 选甲-2 就等于接受多一个渲染面，补偿控制是：默认让它把 Office 渲染成 PDF（容器侧
 * {@code KK_OFFICE_PREVIEW_TYPE=pdf}）、前端沙箱不给 {@code allow-same-origin}、
 * {@code /preview} 经 nginx 只放行 GET/HEAD、关掉它的上传与删除。
 *
 * <p>装配与甲-1 互斥：{@code zhush.preview.office-converter=kkfileview}。
 *
 * @author zhush
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "zhush.preview", name = "office-converter", havingValue = "kkfileview")
public class KkFileViewPreviewRenderer implements RemotePreviewRenderer {

    /**
     * 能交给它渲染的格式：{@link #SERVER_ONLY} 加上「前端本就能渲染、但运维显式偏好服务端高保真」的 docx/xlsx。
     *
     * <p>图片、视频、md/txt、pdf 明确<b>不在</b>内（多一次回源与排队，收益为负，方案 §3.3）；
     * html/htm/svg 也<b>永不</b>在内——可携带脚本的产物交给外部渲染器等于在本系统的源里多开一个
     * 可执行内容出口（方案 S1）。
     */
    private static final Set<String> RENDERABLE = Set.of(
            "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "wps", "et", "dps", "odt", "ods", "odp", "rtf", "vsd",
            "ofd", "xmind", "bpmn", "epub", "eml",
            "zip", "rar", "7z", "tar", "gz", "dwg");

    /** 前端渲染面覆盖不了的格式：命中即改走外部服务，无需运维再在 prefer-pdf-extensions 里显式开口 */
    private static final Set<String> SERVER_ONLY = Set.of(
            "doc", "xls", "ppt", "pptx", "wps", "et", "dps",
            "odt", "ods", "odp", "rtf", "vsd", "ofd", "xmind", "bpmn", "epub", "eml",
            "zip", "rar", "7z", "tar", "gz", "dwg");

    /** 本地存储静态前缀标记，与 {@code RemoteFileFetcher} 同一口径 */
    private static final String LOCAL_URL_MARKER = "/files/";

    private static final int PROBE_CONNECT_MS = 2_000;
    private static final int PROBE_READ_MS = 3_000;
    /** 判定不可达后的重试间隔：容器比后端晚起、或中途重启时不必重启 zhush-server */
    private static final long PROBE_RETRY_MS = 60_000;

    private final PreviewProperties properties;
    private final RemoteFileFetcher fileFetcher;
    private final AtomicBoolean probing = new AtomicBoolean();

    private volatile boolean available;
    private volatile long nextProbeAt;

    public KkFileViewPreviewRenderer(PreviewProperties properties, RemoteFileFetcher fileFetcher) {
        this.properties = properties;
        this.fileFetcher = fileFetcher;
    }

    @PostConstruct
    void init() {
        String sourceBase = properties.getSourceBaseUrl();
        String probeUrl = properties.getProbeUrl();
        log.info("[预览] kkFileView 已装配：渲染前缀={}, 源前缀={}, 可达性探测={}",
                properties.getBaseUrl(),
                sourceBase == null || sourceBase.isBlank() ? "未配（直接用产物原地址）" : sourceBase,
                probeUrl == null || probeUrl.isBlank() ? "未配（不做可达性判断，服务挂了由 iframe 自己报错）" : probeUrl);
        probe();
    }

    @Override
    public String name() {
        return "kkfileview";
    }

    /**
     * 未恢复时按 {@code PROBE_RETRY_MS} 节流重探，探一次只放一个线程，避免服务真挂了时每个请求都去敲一遍。
     */
    @Override
    public boolean available() {
        if (available) {
            return true;
        }
        if (System.currentTimeMillis() < nextProbeAt || !probing.compareAndSet(false, true)) {
            return available;
        }
        try {
            probe();
        } finally {
            probing.set(false);
        }
        return available;
    }

    @Override
    public boolean supports(String ext) {
        return ext != null && RENDERABLE.contains(ext.toLowerCase());
    }

    @Override
    public boolean serverOnly(String ext) {
        return ext != null && SERVER_ONLY.contains(ext.toLowerCase());
    }

    @Override
    public String renderUrl(RenderTarget target) {
        if (!available()) {
            throw new PreviewConversionException(PreviewConversionException.Kind.UNAVAILABLE,
                    "kkFileView 不可用：" + properties.getProbeUrl());
        }
        String source = resolveSourceUrl(target);
        // kkFileView 的 url 参数规则：绝对地址先 Base64，再 URLEncode（Base64 里的 + / = 不编码会被 query 吃掉）
        String encoded = URLEncoder.encode(Base64.getEncoder().encodeToString(
                source.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        return trimTrailingSlash(properties.getBaseUrl()) + "/onlinePreview?url=" + encoded
                + "&fullfilename=" + URLEncoder.encode(fileName(target), StandardCharsets.UTF_8);
    }

    // ================ 内部 ================

    /**
     * 重建 kkFileView 视角可达的绝对源地址（整合方案「决策一」）。
     *
     * <p>绝对地址只放行「运维配置里认得的」——本系统存储域名或 {@code source-base-url} 前缀，其余拒绝：
     * 否则等于让外部服务替我们回源一个任意 URL，SSRF 面就落到了预览链路上（方案 S9）。
     */
    private String resolveSourceUrl(RenderTarget target) {
        String url = target.sourceUrl();
        if (url == null || url.isBlank()) {
            throw new PreviewConversionException(PreviewConversionException.Kind.FAILED, "产物地址为空");
        }
        String trimmed = url.trim();
        String sourceBase = trimTrailingSlash(properties.getSourceBaseUrl());
        if (isHttp(trimmed)) {
            boolean trusted = fileFetcher.isFromStorage(trimmed)
                    || (sourceBase != null && trimmed.startsWith(sourceBase));
            if (!trusted) {
                throw new PreviewConversionException(PreviewConversionException.Kind.FAILED,
                        "产物地址不属于本系统存储，拒绝交给外部渲染服务：" + hostOf(trimmed));
            }
            return trimmed;
        }
        if (sourceBase == null) {
            throw new PreviewConversionException(PreviewConversionException.Kind.FAILED,
                    "相对产物地址需配置 zhush.preview.source-base-url 才能被外部服务回源");
        }
        int marker = trimmed.indexOf(LOCAL_URL_MARKER);
        if (marker < 0) {
            throw new PreviewConversionException(PreviewConversionException.Kind.FAILED,
                    "无法在产物地址中定位存储前缀 " + LOCAL_URL_MARKER);
        }
        String relative = trimmed.substring(marker + LOCAL_URL_MARKER.length());
        while (relative.startsWith("/")) {
            relative = relative.substring(1);
        }
        if (relative.isBlank() || relative.contains("..")) {
            throw new PreviewConversionException(PreviewConversionException.Kind.FAILED, "产物存储路径非法");
        }
        return sourceBase + "/" + relative;
    }

    /** 展示名兼作 kkFileView 的格式判据：无扩展名时补上，否则它只能按 url 尾段猜 */
    private String fileName(RenderTarget target) {
        String name = target.name();
        if (name == null || name.isBlank()) {
            return "preview." + target.ext();
        }
        String trimmed = name.trim();
        return trimmed.contains(".") ? trimmed : trimmed + "." + target.ext();
    }

    private void probe() {
        String probeUrl = properties.getProbeUrl();
        if (probeUrl == null || probeUrl.isBlank()) {
            // base-url 是同源相对前缀时，本进程无从得知浏览器解析出来的那个源；不做判断，交给前端 iframe 自己失败
            available = true;
            return;
        }
        HttpURLConnection conn = null;
        try {
            String target = probeUrl.trim();
            if (!isHttp(target)) {
                markUnavailable("probe-url 需为 http(s) 地址");
                return;
            }
            conn = (HttpURLConnection) URI.create(target).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(PROBE_CONNECT_MS);
            conn.setReadTimeout(PROBE_READ_MS);
            // 与 RemoteFileFetcher 同一口径：不跟重定向，探测只关心服务在不在
            conn.setInstanceFollowRedirects(false);
            int status = conn.getResponseCode();
            if (status >= 200 && status < 400) {
                available = true;
                return;
            }
            markUnavailable("HTTP " + status);
        } catch (IllegalArgumentException e) {
            markUnavailable("probe-url 不是合法地址：" + e.getMessage());
        } catch (IOException e) {
            markUnavailable(e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void markUnavailable(String detail) {
        available = false;
        nextProbeAt = System.currentTimeMillis() + PROBE_RETRY_MS;
        log.warn("[预览] kkFileView 不可用（{}），相关格式回落「仅下载」；确认容器已起、"
                + "zhush.preview.probe-url 指向实际地址（60s 后自动重试）", detail);
    }

    private boolean isHttp(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    /** 日志只出主机名：产物地址可能带签名参数，整条打进日志等于抄一枚 token */
    private String hostOf(String url) {
        try {
            String host = URI.create(url).getHost();
            return host == null ? "无法解析" : host;
        } catch (IllegalArgumentException e) {
            return "无法解析";
        }
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        while (trimmed.length() > 1 && trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

}
