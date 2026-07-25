package com.zhush.biz.ip.config;

import cn.hutool.core.util.StrUtil;
import com.zhush.biz.ip.core.IpRegionUtils;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.service.Config;
import org.lionsoul.ip2region.service.ConfigBuilder;
import org.lionsoul.ip2region.service.Ip2Region;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;

/**
 * IP 归属地自动配置
 *
 * <p>启动时将 IPv4 / IPv6 的 xdb 离线库加载到内存（BufferCache 全内存缓存，天生并发安全），
 * 并注入 {@link IpRegionUtils} 供静态方式调用；也可直接注入 {@link Ip2Region} Bean 使用。
 *
 * @author zhush
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(IpRegionProperties.class)
@ConditionalOnProperty(prefix = "zhush.ip", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IpRegionAutoConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public Ip2Region ip2Region(IpRegionProperties properties, ResourceLoader resourceLoader) {
        try {
            Config v4Config = buildConfig(resourceLoader, properties.getV4XdbPath(), true);
            Config v6Config = buildConfig(resourceLoader, properties.getV6XdbPath(), false);
            Ip2Region ip2Region = Ip2Region.create(v4Config, v6Config);
            IpRegionUtils.init(ip2Region);
            log.info("[ip2Region][初始化完成，v4Xdb({})、v6Xdb({})]",
                    properties.getV4XdbPath(), properties.getV6XdbPath());
            return ip2Region;
        } catch (Exception e) {
            throw new IllegalStateException("初始化 ip2region 查询服务失败", e);
        }
    }

    /**
     * 构建单个 IP 版本的查询配置
     *
     * @param xdbPath xdb 文件位置，为空则返回 {@code null}（该版本的查询返回 null）
     * @param v4      是否为 IPv4 配置
     */
    private Config buildConfig(ResourceLoader resourceLoader, String xdbPath, boolean v4) throws Exception {
        if (StrUtil.isBlank(xdbPath)) {
            return null;
        }
        // xdb 打包在 jar 内无法以文件方式随机读取，统一以流方式全量加载到内存
        InputStream inputStream = resourceLoader.getResource(xdbPath).getInputStream();
        ConfigBuilder builder = Config.custom()
                .setCachePolicy(Config.BufferCache)
                .setXdbInputStream(inputStream);
        return v4 ? builder.asV4() : builder.asV6();
    }

}
