package com.zhush.job.config;

import com.zhush.job.core.JobUtils;
import com.zhush.test.StarterTestSupport;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;

import static org.mockito.Mockito.mock;

/**
 * 定时任务 starter 装配冒烟测试（开关开/关）
 *
 * @author zhush
 */
class JobAutoConfigurationTest extends StarterTestSupport {

    @Test
    void disabledByDefault() {
        assertNoBean(runner(JobAutoConfiguration.class), JobUtils.class);
    }

    @Test
    void disabledExplicitly() {
        assertNoBean(runner(JobAutoConfiguration.class)
                .withPropertyValues("zhush.job.enabled=false"), JobUtils.class);
    }

    @Test
    void enabledRegistersJobUtils() {
        assertSingleBean(runner(JobAutoConfiguration.class)
                .withPropertyValues("zhush.job.enabled=true")
                .withBean(Scheduler.class, () -> mock(Scheduler.class)), JobUtils.class);
    }
}
