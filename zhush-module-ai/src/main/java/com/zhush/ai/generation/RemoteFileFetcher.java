package com.zhush.ai.generation;

import com.zhush.agentscope.config.AgentScopeProperties;
import com.zhush.file.config.FileStorageProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 受限文件取回器：把「按白名单下载远端文件 / 按相对路径读本地存储文件」的防 SSRF 与
 * 目录穿越逻辑集中到一处，供 {@code parseDocument} 工具与图像/视频生成产物转存共用。
 *
 * <p>两条独立白名单，语义不同、不可互换：
 * <ul>
 *     <li>{@link #fetchFromStorage} —— 只认<b>本项目文件存储</b>域名（{@code zhush.file.*.domain}）
 *     或本地存储根下的相对路径，用于读取用户上传后回传的附件地址；</li>
 *     <li>{@link #fetchFromAllowedHosts} —— 只认 {@code zhush.agentscope.generation.allowed-hosts}
 *     列出的<b>第三方供应商</b>域名（如 DashScope OSS），用于转存生成产物。</li>
 * </ul>
 *
 * <p>共同约束：仅 http(s)、禁止跟随重定向、连接/读取超时、体积受 {@code zhush.file.max-size} 限制。
 *
 * @author zhush
 */
@Component
public class RemoteFileFetcher {

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 20_000;
    private static final String LOCAL_URL_MARKER = "/files/";

    private final FileStorageProperties storageProperties;
    private final AgentScopeProperties agentscopeProperties;

    public RemoteFileFetcher(FileStorageProperties storageProperties,
                             AgentScopeProperties agentscopeProperties) {
        this.storageProperties = storageProperties;
        this.agentscopeProperties = agentscopeProperties;
    }

    /**
     * 读取本项目存储内的文件：http(s) 需命中存储域名前缀，相对路径按本地存储根解析。
     */
    public byte[] fetchFromStorage(String urlOrPath) throws IOException {
        if (urlOrPath == null || urlOrPath.isBlank()) {
            throw new IOException("文件地址不能为空");
        }
        String target = urlOrPath.trim();
        if (isHttp(target)) {
            if (matchedStorageDomain(target) == null) {
                throw new IOException("文件地址不在允许的存储域名白名单内，拒绝访问");
            }
            return download(target);
        }
        return readLocal(target);
    }

    /**
     * 下载第三方供应商产物：仅允许 {@code zhush.agentscope.generation.allowed-hosts} 中的主机
     * （含其子域），其余一律拒绝。
     */
    public byte[] fetchFromAllowedHosts(String url) throws IOException {
        if (url == null || url.isBlank()) {
            throw new IOException("文件地址不能为空");
        }
        String target = url.trim();
        if (!isHttp(target)) {
            throw new IOException("生成产物地址必须是 http(s) 链接");
        }
        if (!isHostAllowed(target)) {
            throw new IOException("产物下载地址不在允许的域名白名单内，拒绝访问");
        }
        return download(target);
    }

    // ================ 内部 ================

    private boolean isHttp(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    private boolean isHostAllowed(String url) {
        String host;
        try {
            host = URI.create(url).getHost();
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (host == null) {
            return false;
        }
        String lowerHost = host.toLowerCase();
        List<String> allowed = agentscopeProperties.getGeneration().getAllowedHosts();
        if (allowed == null || allowed.isEmpty()) {
            return false;
        }
        for (String item : allowed) {
            if (item == null || item.isBlank()) {
                continue;
            }
            String normalized = item.trim().toLowerCase();
            // 允许配置成 URL 前缀形式：只取其中的 host 段参与比对
            if (normalized.contains("/")) {
                String stripped = normalized.replaceFirst("^https?://", "");
                int slash = stripped.indexOf('/');
                normalized = slash >= 0 ? stripped.substring(0, slash) : stripped;
            }
            if (lowerHost.equals(normalized) || lowerHost.endsWith("." + normalized)) {
                return true;
            }
        }
        return false;
    }

    private byte[] download(String fileUrl) throws IOException {
        URL url = URI.create(fileUrl).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        // 关闭重定向：否则可用 302 跳转到内网地址绕过白名单
        conn.setInstanceFollowRedirects(false);
        try {
            int status = conn.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IOException("文件下载失败，HTTP 状态码：" + status);
            }
            long declared = conn.getContentLengthLong();
            if (declared > storageProperties.getMaxSize()) {
                throw new IOException("文件超出大小限制");
            }
            try (InputStream in = conn.getInputStream()) {
                byte[] bytes = in.readAllBytes();
                if (bytes.length > storageProperties.getMaxSize()) {
                    throw new IOException("文件超出大小限制");
                }
                return bytes;
            }
        } finally {
            conn.disconnect();
        }
    }

    private byte[] readLocal(String relativeUrl) throws IOException {
        FileStorageProperties.Local local = storageProperties.getLocal();
        // 去掉静态资源映射前缀（如 /files/），得到相对存储根的路径（可能含日期子目录），防 ../ 穿越
        String partialPath = relativeUrl;
        int marker = partialPath.indexOf(LOCAL_URL_MARKER);
        if (marker >= 0) {
            partialPath = partialPath.substring(marker + LOCAL_URL_MARKER.length());
        }
        while (partialPath.startsWith("/")) {
            partialPath = partialPath.substring(1);
        }
        Path base = Path.of(local.getBasePath()).toAbsolutePath().normalize();
        Path target = base.resolve(partialPath).normalize();
        if (!target.startsWith(base)) {
            throw new IOException("非法的文件路径");
        }
        if (!Files.isRegularFile(target)) {
            throw new IOException("本地文件不存在：" + target.getFileName());
        }
        if (Files.size(target) > storageProperties.getMaxSize()) {
            throw new IOException("文件超出大小限制");
        }
        return Files.readAllBytes(target);
    }

    /**
     * 返回命中的允许存储域名前缀（local domain 及各启用平台 domain），未命中返回 null。
     */
    private String matchedStorageDomain(String fileUrl) {
        String localDomain = storageProperties.getLocal().getDomain();
        if (isNonPlaceholder(localDomain) && fileUrl.startsWith(localDomain)) {
            return localDomain;
        }
        if (storageProperties.getAliyunOss().isEnabled()
                && isNonPlaceholder(storageProperties.getAliyunOss().getDomain())
                && fileUrl.startsWith(storageProperties.getAliyunOss().getDomain())) {
            return storageProperties.getAliyunOss().getDomain();
        }
        if (storageProperties.getQiniu().isEnabled()
                && isNonPlaceholder(storageProperties.getQiniu().getDomain())
                && fileUrl.startsWith(storageProperties.getQiniu().getDomain())) {
            return storageProperties.getQiniu().getDomain();
        }
        if (storageProperties.getHuaweiObs().isEnabled()
                && isNonPlaceholder(storageProperties.getHuaweiObs().getDomain())
                && fileUrl.startsWith(storageProperties.getHuaweiObs().getDomain())) {
            return storageProperties.getHuaweiObs().getDomain();
        }
        return null;
    }

    private static boolean isNonPlaceholder(String domain) {
        return domain != null && !domain.isBlank() && !domain.contains("your-domain");
    }
}
