package com.zhush.biz.ip.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * IP 归属地离线库配置项
 *
 * <p>前缀：{@code zhush.ip}
 *
 * @author zhush
 */
@Data
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

}
