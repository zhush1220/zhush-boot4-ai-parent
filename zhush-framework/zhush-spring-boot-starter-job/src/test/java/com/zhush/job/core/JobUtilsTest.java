package com.zhush.job.core;

import com.zhush.job.config.JobProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JobUtils 定时任务操作封装单测（Mockito 模拟 Scheduler，不依赖真实调度器）
 *
 * @author zhush
 */
@ExtendWith(MockitoExtension.class)
class JobUtilsTest {

    @Mock
    private Scheduler scheduler;

    private JobUtils jobUtils;

    @BeforeEach
    void setUp() {
        JobProperties properties = new JobProperties();
        jobUtils = new JobUtils(scheduler, properties);
    }

    @Test
    void addCronJobSchedulesWithDefaultGroup() throws Exception {
        jobUtils.addCronJob(TestJob.class, "job1", null, "0 0/5 * * * ?", Map.of("k", "v"));

        ArgumentCaptor<JobDetail> detailCaptor = ArgumentCaptor.forClass(JobDetail.class);
        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
        verify(scheduler).scheduleJob(detailCaptor.capture(), triggerCaptor.capture());

        JobDetail detail = detailCaptor.getValue();
        assertThat(detail.getKey()).isEqualTo(JobKey.jobKey("job1", "DEFAULT"));
        assertThat(detail.getJobClass()).isEqualTo(TestJob.class);
        assertThat(detail.getJobDataMap()).containsEntry("k", "v");

        CronTrigger trigger = (CronTrigger) triggerCaptor.getValue();
        assertThat(trigger.getKey()).isEqualTo(TriggerKey.triggerKey("job1", "DEFAULT"));
        assertThat(trigger.getCronExpression()).isEqualTo("0 0/5 * * * ?");
    }

    @Test
    void addCronJobReplacesExistingJob() throws Exception {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);

        jobUtils.addCronJob(TestJob.class, "job1", "G1", "0 0/5 * * * ?", null);

        verify(scheduler).deleteJob(JobKey.jobKey("job1", "G1"));
        verify(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    @Test
    void addCronJobWithoutParamsKeepsEmptyDataMap() throws Exception {
        jobUtils.addCronJob(TestJob.class, "job1", "G1", "0 0/5 * * * ?", null);

        ArgumentCaptor<JobDetail> detailCaptor = ArgumentCaptor.forClass(JobDetail.class);
        verify(scheduler).scheduleJob(detailCaptor.capture(), any(Trigger.class));
        assertThat(detailCaptor.getValue().getJobDataMap()).isEmpty();
    }

    @Test
    void updateCronReschedulesWhenTriggerExists() throws Exception {
        CronTrigger existing = TriggerBuilder.newTrigger()
                .withIdentity("job1", "G1")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0/5 * * * ?"))
                .build();
        when(scheduler.getTrigger(TriggerKey.triggerKey("job1", "G1"))).thenReturn(existing);

        jobUtils.updateCron("job1", "G1", "0 0 12 * * ?");

        ArgumentCaptor<CronTrigger> triggerCaptor = ArgumentCaptor.forClass(CronTrigger.class);
        verify(scheduler).rescheduleJob(eq(TriggerKey.triggerKey("job1", "G1")), triggerCaptor.capture());
        assertThat(triggerCaptor.getValue().getCronExpression()).isEqualTo("0 0 12 * * ?");
    }

    @Test
    void updateCronSilentlySkipsMissingTrigger() throws Exception {
        when(scheduler.getTrigger(any(TriggerKey.class))).thenReturn(null);

        jobUtils.updateCron("job1", "G1", "0 0 12 * * ?");

        verify(scheduler, never()).rescheduleJob(any(TriggerKey.class), any(Trigger.class));
    }

    @Test
    void pauseResumeDeleteExistsTriggerOnceDelegateWithRealGroup() throws Exception {
        jobUtils.pauseJob("job1", "G1");
        verify(scheduler).pauseJob(JobKey.jobKey("job1", "G1"));

        jobUtils.resumeJob("job1", "G1");
        verify(scheduler).resumeJob(JobKey.jobKey("job1", "G1"));

        when(scheduler.deleteJob(JobKey.jobKey("job1", "G1"))).thenReturn(true);
        assertThat(jobUtils.deleteJob("job1", "G1")).isTrue();
        when(scheduler.deleteJob(JobKey.jobKey("job2", "G1"))).thenReturn(false);
        assertThat(jobUtils.deleteJob("job2", "G1")).isFalse();

        when(scheduler.checkExists(JobKey.jobKey("job1", "G1"))).thenReturn(true);
        assertThat(jobUtils.exists("job1", "G1")).isTrue();
        when(scheduler.checkExists(JobKey.jobKey("job3", "G1"))).thenReturn(false);
        assertThat(jobUtils.exists("job3", "G1")).isFalse();

        jobUtils.triggerOnce("job1", "G1");
        verify(scheduler).triggerJob(JobKey.jobKey("job1", "G1"));
    }

    @Test
    void blankGroupFallsBackToDefaultGroup() throws Exception {
        jobUtils.pauseJob("job1", " ");
        verify(scheduler).pauseJob(JobKey.jobKey("job1", "DEFAULT"));
    }

    @Test
    void getNextFireTimeReturnsNullWhenTriggerMissing() throws Exception {
        when(scheduler.getTrigger(any(TriggerKey.class))).thenReturn(null);
        assertThat(jobUtils.getNextFireTime("job1", "G1")).isNull();
    }

    @Test
    void getNextFireTimeReturnsTriggerTime() throws Exception {
        Trigger trigger = org.mockito.Mockito.mock(Trigger.class);
        Date next = new Date(123456L);
        when(scheduler.getTrigger(any(TriggerKey.class))).thenReturn(trigger);
        when(trigger.getNextFireTime()).thenReturn(next);

        assertThat(jobUtils.getNextFireTime("job1", "G1")).isEqualTo(next);
    }

    @Test
    void schedulerExceptionWrappedAsIllegalStateException() throws Exception {
        when(scheduler.checkExists(any(JobKey.class))).thenThrow(new SchedulerException("boom"));

        assertThatThrownBy(() -> jobUtils.exists("job1", "G1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("查询定时任务失败")
                .hasRootCauseInstanceOf(SchedulerException.class);
    }

    @Test
    void addCronJobExceptionWrappedWithJobKey() throws Exception {
        when(scheduler.checkExists(any(JobKey.class))).thenThrow(new SchedulerException("boom"));

        assertThatThrownBy(() -> jobUtils.addCronJob(TestJob.class, "job1", "G1", "0 0/5 * * * ?", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("新增定时任务失败")
                .hasMessageContaining("G1.job1");
    }

    /** 测试任务类 */
    static class TestJob implements Job {
        @Override
        public void execute(JobExecutionContext context) {
            // 仅用于 JobDetail 类型断言
        }
    }
}
