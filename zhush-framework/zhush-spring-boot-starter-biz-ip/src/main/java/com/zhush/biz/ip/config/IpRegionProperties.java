package com.zhush.biz.ip.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * IP 归属地离线库配置项
 *
 * <p>前缀：{@code zhush.ip}
 *
 * @author zhush
 */
@ConfigurationProperties(prefix = "zhush.ip")
public class IpRegionProperties {

    /**
     * 是否启用 IP 归属地查询
     */
    private boolean enabled = true;

    /**
     * IPv4 xdb 文件位置（支持 classpath: / file: 前缀），留空则不支持 IPv4 查询
     */
    private String v4XdbPath = "classpath:ip2region/ip2region_v4.xdb";

    /**
     * IPv6 xdb 文件位置（支持 classpath: / file: 前缀），留空则不支持 IPv6 查询
     */
    private String v6XdbPath = "classpath:ip2region/ip2region_v6.xdb";

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setV4XdbPath(String v4XdbPath) {
        this.v4XdbPath = v4XdbPath;
    }

    public void setV6XdbPath(String v6XdbPath) {
        this.v6XdbPath = v6XdbPath;
    }

}
