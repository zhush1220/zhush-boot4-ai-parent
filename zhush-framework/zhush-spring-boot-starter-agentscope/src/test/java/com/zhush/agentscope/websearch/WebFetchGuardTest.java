package com.zhush.agentscope.websearch;

import com.zhush.agentscope.config.AgentScopeProperties;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SSRF 闸门测试（方案 6.6）：协议、域名白/黑名单、内网与云元数据地址拦截。
 *
 * <p>用例刻意只用 IP 字面量与非解析类断言：域名解析依赖环境 DNS，不能把「本机能否上网」带进单测。
 */
class WebFetchGuardTest {

    private final AgentScopeProperties.WebSearch.Fetch config = new AgentScopeProperties.WebSearch.Fetch();

    private final WebFetchGuard guard = new WebFetchGuard(config);

    @Test
    void shouldRejectNonHttpScheme() {
        assertThatThrownBy(() -> guard.validate("file:///etc/passwd"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("仅允许抓取 http/https");
        assertThatThrownBy(() -> guard.validate("gopher://127.0.0.1:11211/"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("仅允许抓取 http/https");
    }

    @Test
    void shouldRejectEmptyAndMalformedAddress() {
        assertThatThrownBy(() -> guard.validate("  "))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("地址不能为空");
        assertThatThrownBy(() -> guard.validate("http://"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("格式非法");
        // 有 scheme 无 authority：URI 能建出来但取不到 host
        assertThatThrownBy(() -> guard.validate("http:/only/path"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("无法解析主机名");
    }

    @Test
    void shouldRejectLoopbackLinkLocalAndPrivateIpv4() {
        assertBlockedByAddressCheck("http://127.0.0.1/");
        assertBlockedByAddressCheck("http://127.1.2.3:8080/admin");
        assertBlockedByAddressCheck("http://169.254.169.254/latest/meta-data/");
        assertBlockedByAddressCheck("http://10.0.0.5/");
        assertBlockedByAddressCheck("http://192.168.1.1/");
        assertBlockedByAddressCheck("http://172.16.0.9/");
        assertBlockedByAddressCheck("http://0.0.0.0/");
    }

    @Test
    void shouldRejectIpv6LoopbackAndUla() {
        assertBlockedByAddressCheck("http://[::1]/");
        assertBlockedByAddressCheck("http://[fd00::1]/");
        assertBlockedByAddressCheck("http://[fe80::1]/");
    }

    @Test
    void shouldRejectCgnatWhichJdkSiteLocalMisses() throws Exception {
        // JDK 的 isSiteLocalAddress() 不认 100.64/10，云环境容器网段常落在这一段
        assertThat(WebFetchGuard.isBlockedAddress(InetAddress.getByName("100.64.0.1"))).isTrue();
        assertThat(WebFetchGuard.isBlockedAddress(InetAddress.getByName("100.127.255.255"))).isTrue();
        assertThat(WebFetchGuard.isBlockedAddress(InetAddress.getByName("100.128.0.1"))).isFalse();
        assertBlockedByAddressCheck("http://100.64.0.1/");
    }

    @Test
    void shouldNotOverBlockPublicRangesNextToReservedOnes() throws Exception {
        assertThat(WebFetchGuard.isBlockedAddress(InetAddress.getByName("8.8.8.8"))).isFalse();
        assertThat(WebFetchGuard.isBlockedAddress(InetAddress.getByName("192.0.1.1"))).isFalse();
        assertThat(WebFetchGuard.isBlockedAddress(InetAddress.getByName("192.2.0.1"))).isFalse();
        assertThat(WebFetchGuard.isBlockedAddress(InetAddress.getByName("172.15.0.1"))).isFalse();
        assertThat(WebFetchGuard.isBlockedAddress(InetAddress.getByName("100.63.255.1"))).isFalse();
    }

    @Test
    void shouldUnwrapIpv4MappedIpv6BeforeBlocking() throws Exception {
        assertThat(WebFetchGuard.isBlockedAddress(InetAddress.getByName("::ffff:127.0.0.1"))).isTrue();
        assertThat(WebFetchGuard.isBlockedAddress(InetAddress.getByName("::ffff:8.8.8.8"))).isFalse();
    }

    @Test
    void denyListShouldRejectBeforeAddressCheck() {
        config.setDomainDenyList(List.of("example.com"));

        // 未列入黑名单的域名需要真实 DNS 才能判定，黑名单命中必须在解析前短路
        assertThatThrownBy(() -> guard.validate("https://a.example.com/secret"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("不在允许抓取的范围内");
    }

    @Test
    void emptyAllowListMeansNoWhitelistMode() throws IOException {
        config.setDomainAllowList(List.of());

        // 白名单为空即不启用白名单模式：仍由地址检查兜底（此处指向内网，应被拒）
        assertBlockedByAddressCheck("http://127.0.0.1/");
    }

    @Test
    void allowListShouldRejectUnlistedDomain() {
        config.setDomainAllowList(List.of("docs.example.com"));

        assertThatThrownBy(() -> guard.validate("http://127.0.0.1/"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("未列入抓取白名单");
    }

    @Test
    void allowListShouldMatchItselfAndSubDomainOnly() throws IOException {
        // 关掉地址检查，单独验域名后缀语义：域名用例因此不依赖环境 DNS
        config.setAllowPrivateNetwork(true);
        config.setDomainAllowList(List.of("example.com"));

        assertThat(guard.validate("https://example.com/a")).isNotNull();
        assertThat(guard.validate("https://docs.example.com/b")).isNotNull();
        assertThatThrownBy(() -> guard.validate("https://notexample.com/"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("未列入抓取白名单");
        // 后缀伪装域名不得被当成子域放行
        assertThatThrownBy(() -> guard.validate("https://example.com.evil.net/"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("未列入抓取白名单");
    }

    @Test
    void allowPrivateNetworkShouldSkipAddressCheck() throws IOException {
        config.setAllowPrivateNetwork(true);

        assertThat(guard.validate("http://127.0.0.1:8080/")).isNotNull();
    }

    private void assertBlockedByAddressCheck(String url) {
        assertThatThrownBy(() -> guard.validate(url))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("内网或保留地址");
    }
}
