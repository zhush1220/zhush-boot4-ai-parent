package com.zhush.agentscope.websearch;

import com.zhush.agentscope.config.AgentScopeProperties;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;

/**
 * 抓取安全闸门（方案 6.6 的 SSRF 行）：协议、域名白/黑名单与「解析后 IP 必须为公网地址」三道判断。
 *
 * <p>为何必须逐跳重查：本项目容器以 {@code network_mode: host} + {@code privileged} 运行，
 * 一次 302 指向 {@code 169.254.169.254} 或 {@code 127.0.0.1:8080} 就等于把主机内网交给模型读写，
 * 只在首跳校验等于没校验。
 *
 * <p><b>残留风险（需部署侧配合）</b>：本类校验的是「校验时刻」的 DNS 结果，JDK HttpClient 连接时
 * 会重新解析主机名，理论上存在 DNS 重绑定（TOCTOU）窗口。收紧方式是把出网收敛到固定 egress 代理，
 * 或以 {@code java.net.spi.InetAddressResolverProvider} 在全 JVM 范围过滤解析结果；
 * 应用层无法单独关闭该窗口，故 {@code allow-private-network} 之外还应配合网络策略。
 *
 * @author zhush
 */
public class WebFetchGuard {

    private final AgentScopeProperties.WebSearch.Fetch fetchConfig;

    public WebFetchGuard(AgentScopeProperties.WebSearch.Fetch fetchConfig) {
        this.fetchConfig = fetchConfig;
    }

    /**
     * 校验一个待抓取的绝对地址：不合法即抛 {@link IOException}，消息面向模型可读、不外泄内网探测细节。
     *
     * @param url 待抓取地址
     * @return 解析后的 URI（便于调用方复用 host 判断）
     */
    public URI validate(String url) throws IOException {
        URI uri = parse(url);
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IOException("目标地址无法解析主机名，已拒绝抓取");
        }
        assertDomainAllowed(uri.getHost());
        assertAddressPublic(uri.getHost());
        return uri;
    }

    /**
     * 仅做协议与格式判断（不含 DNS），供入口先快速拒绝明显非法地址。
     */
    public URI parse(String url) throws IOException {
        if (url == null || url.isBlank()) {
            throw new IOException("地址不能为空");
        }
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException e) {
            throw new IOException("目标地址格式非法，已拒绝抓取");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new IOException("仅允许抓取 http/https 地址");
        }
        return uri;
    }

    private void assertDomainAllowed(String host) throws IOException {
        String lower = host.toLowerCase(Locale.ROOT);
        List<String> deny = fetchConfig.getDomainDenyList();
        if (deny != null) {
            for (String item : deny) {
                if (matches(lower, item)) {
                    throw new IOException("目标地址不在允许抓取的范围内");
                }
            }
        }
        List<String> allow = fetchConfig.getDomainAllowList();
        // 白名单非空即为白名单模式：未列入一律拒绝
        if (allow != null && !allow.isEmpty()) {
            for (String item : allow) {
                if (matches(lower, item)) {
                    return;
                }
            }
            throw new IOException("目标域名未列入抓取白名单");
        }
    }

    private void assertAddressPublic(String host) throws IOException {
        if (fetchConfig.isAllowPrivateNetwork()) {
            return;
        }
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new IOException("目标域名解析失败");
        }
        for (InetAddress address : addresses) {
            if (isBlockedAddress(address)) {
                // 不回显具体 IP：内网可达性本身就是探测结果，不能让模型据此测绘
                throw new IOException("目标地址指向内网或保留地址，已拒绝抓取");
            }
        }
    }

    /**
     * 域名是否命中配置项（后缀匹配：{@code example.com} 同时覆盖自身与其子域）。
     */
    private static boolean matches(String host, String item) {
        if (item == null || item.isBlank()) {
            return false;
        }
        String normalized = item.trim().toLowerCase(Locale.ROOT);
        int slash = normalized.indexOf('/');
        if (slash >= 0) {
            normalized = normalized.substring(0, slash);
        }
        normalized = normalized.replaceFirst("^https?://", "");
        return host.equals(normalized) || host.endsWith("." + normalized);
    }

    /**
     * 是否为不可达的内网/保留地址。
     *
     * <p>JDK 的 {@code isSiteLocalAddress()} 不覆盖 IPv6 ULA（fc00::/7）与 IPv4 CGNAT（100.64/10），
     * 而云环境常用这两段，故按字节前缀补判；IPv4-mapped IPv6 先还原成 4 字节再判。
     */
    static boolean isBlockedAddress(InetAddress address) {
        if (address == null) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (bytes == null) {
            return true;
        }
        if (address instanceof Inet6Address && isIpv4Mapped(bytes)) {
            byte[] v4 = new byte[]{bytes[12], bytes[13], bytes[14], bytes[15]};
            return isBlockedIpv4(v4) || address.isLoopbackAddress();
        }
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return true;
        }
        if (bytes.length == 4) {
            return isBlockedIpv4(bytes);
        }
        // IPv6 ULA fc00::/7（JDK isSiteLocalAddress 只认已废弃的 fec0::/10）
        return bytes.length == 16 && (bytes[0] & 0xFE) == 0xFC;
    }

    private static boolean isIpv4Mapped(byte[] bytes) {
        if (bytes.length != 16) {
            return false;
        }
        for (int i = 0; i < 10; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        return bytes[10] == (byte) 0xFF && bytes[11] == (byte) 0xFF;
    }

    /**
     * IPv4 内网/保留段判断（首字节 + 掩码）。
     */
    private static boolean isBlockedIpv4(byte[] b) {
        int o0 = b[0] & 0xFF;
        int o1 = b[1] & 0xFF;
        return switch (o0) {
            // 0/8 这一段本身含「本机」语义，127/8 回环，240/4+ 与广播保留
            case 0, 10, 127 -> true;
            // 100.64/10 CGNAT：云厂商元数据与容器网段常用
            case 100 -> (o1 & 0xC0) == 64;
            // 169.254/16 链路本地：云元数据地址 169.254.169.254 在此
            case 169 -> o1 == 254;
            case 172 -> (o1 >= 16 && o1 <= 31);
            // 192.0.0.0/24（IETF 协议分配）与 192.0.2.0/24（TEST-NET-1）保留，192.168/16 私网
            case 192 -> o1 == 168 || (o1 == 0 && ((b[2] & 0xFF) == 0 || (b[2] & 0xFF) == 2));
            // 198.18.0.0/15 设备测试、198.51.100.0/24 文档段
            case 198 -> (o1 & 0xFE) == 18 || o1 == 51;
            case 203 -> o1 == 113;
            // 224/4 组播、240/4 保留
            default -> o0 >= 224;
        };
    }
}
