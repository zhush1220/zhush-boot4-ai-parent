package com.zhush.job.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * 定时任务环境后置处理器
 *
 * <p>Boot 的 Quartz 自动配置不受 {@code zhush.job.enabled} 约束，
 * 为保证「默认关闭、零运行开销」的启停语义：当任务开关关闭时，
 * 自动补充 {@code spring.quartz.auto-startup=false}（用户显式配置优先），
 * 使调度器不启动、任务不执行。
 *
 * @author zhush
 */
public class JobEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "zhushJobDefaults";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String enabled = environment.getProperty(JobProperties.PREFIX + ".enabled", "false");
        if (Boolean.parseBoolean(enabled)) {
            return;
        }
        // 用户已显式配置 auto-startup 时不覆盖
        if (environment.containsProperty("spring.quartz.auto-startup")) {
            return;
        }
        environment.getPropertySources().addLast(
                new MapPropertySource(PROPERTY_SOURCE_NAME, Map.of("spring.quartz.auto-startup", "false")));
    }
}
