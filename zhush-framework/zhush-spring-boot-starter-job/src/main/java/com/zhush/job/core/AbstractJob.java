package com.zhush.job.core;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * 定时任务基类
 *
 * <p>统一处理执行耗时日志与异常包装；业务任务继承本类并实现
 * {@link #doExecute(JobExecutionContext)}，任务类中可直接
 * {@code @Autowired} 注入 Spring Bean（Boot 已装配支持依赖注入的 JobFactory）。
 *
 * <p>任务参数通过 {@code JobUtils.addCronJob} 的 params 传入，
 * 执行时经 {@code context.getMergedJobDataMap()} 获取。
 *
 * @author zhush
 */
@Slf4j
public abstract class AbstractJob implements Job {

    @Override
    public final void execute(JobExecutionContext context) throws JobExecutionException {
        String jobKey = context.getJobDetail().getKey().toString();
        long start = System.currentTimeMillis();
        try {
            doExecute(context);
            log.debug("定时任务执行成功：{}，耗时 {}ms", jobKey, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("定时任务执行失败：{}，耗时 {}ms", jobKey, System.currentTimeMillis() - start, e);
            throw new JobExecutionException(e);
        }
    }

    /**
     * 业务执行逻辑
     *
     * @param context 任务上下文（含 JobDataMap 参数、触发时间等）
     * @throws Exception 业务异常将被记录日志并向调度器上抛
     */
    protected abstract void doExecute(JobExecutionContext context) throws Exception;
}
