package com.zhush.job.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 定时任务配置属性
 *
 * <p>Quartz 原生配置（线程池、存储方式、集群等）仍走 Spring 标准
 * {@code spring.quartz.*}，本属性仅管理 starter 自身的启停与默认值。
 *
 * @author zhush
 */
@Data
@ConfigurationProperties(prefix = JobProperties.PREFIX)
public class JobProperties {

    public static final String PREFIX = "zhush.job";

    /**
     * 是否启用定时任务（总开关，默认关闭；
     * 关闭时 Boot 的 Quartz 调度器不会启动，零运行开销）
     */
    private Boolean enabled = false;

    /**
     * 未指定分组时的默认任务分组
     */
    private String defaultGroup = "DEFAULT";
}
