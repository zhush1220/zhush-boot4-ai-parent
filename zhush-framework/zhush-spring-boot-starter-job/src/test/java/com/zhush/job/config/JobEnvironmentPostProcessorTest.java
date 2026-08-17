package com.zhush.job.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 定时任务环境后置处理器单测（默认关闭 → 补充 quartz 不自动启动）
 *
 * @author zhush
 */
class JobEnvironmentPostProcessorTest {

    private final JobEnvironmentPostProcessor processor = new JobEnvironmentPostProcessor();

    @Test
    void defaultDisabledAddsAutoStartupFalse() {
        MockEnvironment environment = new MockEnvironment();
        processor.postProcessEnvironment(environment, null);
        assertThat(environment.getProperty("spring.quartz.auto-startup")).isEqualTo("false");
    }

    @Test
    void enabledDoesNotTouchQuartzStartup() {
        MockEnvironment environment = new MockEnvironment().withProperty("zhush.job.enabled", "true");
        processor.postProcessEnvironment(environment, null);
        assertThat(environment.getProperty("spring.quartz.auto-startup")).isNull();
    }

    @Test
    void userExplicitAutoStartupWins() {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.quartz.auto-startup", "true");
        processor.postProcessEnvironment(environment, null);
        assertThat(environment.getProperty("spring.quartz.auto-startup")).isEqualTo("true");
    }
}
