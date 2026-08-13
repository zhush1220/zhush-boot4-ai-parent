package com.zhush.job.core;

import com.zhush.job.config.JobProperties;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

import java.util.Date;
import java.util.Map;

/**
 * 定时任务操作封装
 *
 * <p>基于 {@link Scheduler} 提供任务的增删改查与手动触发能力，
 * 统一使用「任务类 + 名称 + 分组 + Cron」定位任务，异常统一转为
 * {@link IllegalStateException} 简化调用方处理。
 *
 * @author zhush
 */
public class JobUtils {

    private final Scheduler scheduler;

    private final JobProperties properties;

    public JobUtils(Scheduler scheduler, JobProperties properties) {
        this.scheduler = scheduler;
        this.properties = properties;
    }

    /**
     * 新增或替换 Cron 任务（同名同组已存在时先删除再重建）
     *
     * @param jobClass 任务类（建议继承 {@link AbstractJob}）
     * @param jobName  任务名称
     * @param group    分组（null 时使用默认分组）
     * @param cron     Cron 表达式
     * @param params   任务参数（可为 null）
     */
    public void addCronJob(Class<? extends Job> jobClass, String jobName, String group,
                           String cron, Map<String, Object> params) {
        String realGroup = realGroup(group);
        JobKey jobKey = JobKey.jobKey(jobName, realGroup);
        try {
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }
            JobDataMap dataMap = new JobDataMap();
            if (params != null) {
                dataMap.putAll(params);
            }
            JobDetail jobDetail = JobBuilder.newJob(jobClass)
                    .withIdentity(jobKey)
                    .usingJobData(dataMap)
                    .build();
            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(TriggerKey.triggerKey(jobName, realGroup))
                    .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                    .build();
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            throw new IllegalStateException("新增定时任务失败: " + jobKey, e);
        }
    }

    /**
     * 新增或替换 Cron 任务（默认分组、无参数）
     */
    public void addCronJob(Class<? extends Job> jobClass, String jobName, String cron) {
        addCronJob(jobClass, jobName, null, cron, null);
    }

    /**
     * 更新已存在任务的 Cron 表达式（任务不存在时静默返回）
     */
    public void updateCron(String jobName, String group, String cron) {
        String realGroup = realGroup(group);
        TriggerKey triggerKey = TriggerKey.triggerKey(jobName, realGroup);
        try {
            CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
            if (trigger == null) {
                return;
            }
            CronTrigger newTrigger = trigger.getTriggerBuilder()
                    .withIdentity(triggerKey)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                    .build();
            scheduler.rescheduleJob(triggerKey, newTrigger);
        } catch (SchedulerException e) {
            throw new IllegalStateException("更新定时任务 Cron 失败: " + jobName, e);
        }
    }

    /**
     * 暂停任务
     */
    public void pauseJob(String jobName, String group) {
        try {
            scheduler.pauseJob(JobKey.jobKey(jobName, realGroup(group)));
        } catch (SchedulerException e) {
            throw new IllegalStateException("暂停定时任务失败: " + jobName, e);
        }
    }

    /**
     * 恢复任务
     */
    public void resumeJob(String jobName, String group) {
        try {
            scheduler.resumeJob(JobKey.jobKey(jobName, realGroup(group)));
        } catch (SchedulerException e) {
            throw new IllegalStateException("恢复定时任务失败: " + jobName, e);
        }
    }

    /**
     * 删除任务（连同触发器）
     */
    public boolean deleteJob(String jobName, String group) {
        try {
            return scheduler.deleteJob(JobKey.jobKey(jobName, realGroup(group)));
        } catch (SchedulerException e) {
            throw new IllegalStateException("删除定时任务失败: " + jobName, e);
        }
    }

    /**
     * 判断任务是否存在
     */
    public boolean exists(String jobName, String group) {
        try {
            return scheduler.checkExists(JobKey.jobKey(jobName, realGroup(group)));
        } catch (SchedulerException e) {
            throw new IllegalStateException("查询定时任务失败: " + jobName, e);
        }
    }

    /**
     * 立即触发一次已存在的任务（不等待 Cron 周期）
     */
    public void triggerOnce(String jobName, String group) {
        try {
            scheduler.triggerJob(JobKey.jobKey(jobName, realGroup(group)));
        } catch (SchedulerException e) {
            throw new IllegalStateException("手动触发定时任务失败: " + jobName, e);
        }
    }

    /**
     * 获取下次执行时间（任务不存在返回 null）
     */
    public Date getNextFireTime(String jobName, String group) {
        try {
            var trigger = scheduler.getTrigger(TriggerKey.triggerKey(jobName, realGroup(group)));
            return trigger == null ? null : trigger.getNextFireTime();
        } catch (SchedulerException e) {
            throw new IllegalStateException("查询下次执行时间失败: " + jobName, e);
        }
    }

    /**
     * 获取底层 Scheduler（需要未封装能力时使用）
     */
    public Scheduler getScheduler() {
        return scheduler;
    }

    private String realGroup(String group) {
        return group == null || group.isBlank() ? properties.getDefaultGroup() : group;
    }
}
