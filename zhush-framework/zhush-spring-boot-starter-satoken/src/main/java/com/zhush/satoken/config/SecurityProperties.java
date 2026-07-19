package com.zhush.satoken.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全 / 鉴权配置项
 *
 * <p>前缀：{@code zhush.security}
 *
 * @author zhush
 */
@ConfigurationProperties(prefix = "zhush.security")
public class SecurityProperties {

    /**
     * 管理端路径前缀（需登录 + 权限校验）
     */
    private String adminPrefix = "/admin/**";

    /**
     * 会员端路径前缀（仅需登录）
     */
    private String memberPrefix = "/member/**";

    /**
     * 免登录白名单路径（Ant 风格），会与内置白名单合并
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
