package com.zhush.satoken.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全 / 鉴权配置项
 *
 * <p>前缀：{@code zhush.security}
 *
 * <p>路径语义：下列路径按 Sa-Token 的原始 {@code requestURI} 匹配，因此会包含 {@code server.servlet.context-path}。
 * 未配置 context-path 时写 {@code /admin/**} 即可；配置了（如 {@code /api}）建议直接写 {@code /api/admin/**}，
 * 若省略该前缀，{@code SaTokenConfigure} 会自动补全（已带前缀的写法不会重复拼接）。
 *
 * @author zhush
 */
@ConfigurationProperties(prefix = "zhush.security")
public class SecurityProperties {

    /**
     * 管理端路径前缀（需登录 + 权限校验）
     *
     * <p>例：{@code /api/admin/**}（context-path 为 /api 时）
     */
    private String adminPrefix = "/admin/**";

    /**
     * 会员端路径前缀（仅需登录）
     *
     * <p>例：{@code /api/member/**}（context-path 为 /api 时）
     */
    private String memberPrefix = "/member/**";

    /**
     * 免登录白名单路径（Ant 风格），会与内置白名单合并
     *
     * <p>例：{@code /api/admin/auth/login}（context-path 为 /api 时）
     */
    private List<String> ignorePaths = new ArrayList<>();

    public String getAdminPrefix() {
        return adminPrefix;
    }

    public void setAdminPrefix(String adminPrefix) {
        this.adminPrefix = adminPrefix;
    }

    public String getMemberPrefix() {
        return memberPrefix;
    }

    public void setMemberPrefix(String memberPrefix) {
        this.memberPrefix = memberPrefix;
    }

    public List<String> getIgnorePaths() {
        return ignorePaths;
    }

    public void setIgnorePaths(List<String> ignorePaths) {
        this.ignorePaths = ignorePaths;
    }

}
