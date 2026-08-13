package com.zhush.job.config;

import com.zhush.job.core.JobUtils;
import org.quartz.Scheduler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 定时任务自动装配（Quartz）
 *
 * <p>装配内容：
 * <ul>
 *     <li>{@link JobUtils}：任务增删改查、暂停恢复、手动触发的统一操作入口</li>
 * </ul>
 *
 * <p>启停控制：{@code zhush.job.enabled}（默认 false）。关闭时
 * {@code JobEnvironmentPostProcessor} 会补充 {@code spring.quartz.auto-startup=false}，
 * Boot 创建的调度器不会启动，零运行开销。
 *
 * <p>Quartz 自身配置（线程池、JDBC 持久化、集群等）走标准
 * {@code spring.quartz.*}；任务持久化需引入 Quartz 建表脚本
 * 并设置 {@code spring.quartz.job-store-type: jdbc}。
 *
 * @author zhush
 */
@AutoConfiguration
@ConditionalOnClass(Scheduler.class)
@EnableConfigurationProperties(JobProperties.class)
@ConditionalOnProperty(prefix = JobProperties.PREFIX, name = "enabled", havingValue = "true")
public class JobAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JobUtils jobUtils(Scheduler scheduler, JobProperties properties) {
        return new JobUtils(scheduler, properties);
    }
}
