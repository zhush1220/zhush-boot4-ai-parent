package com.zhush.system.job;

import com.zhush.job.core.AbstractJob;
import com.zhush.system.entity.SysJob;
import com.zhush.system.entity.SysJobLog;
import com.zhush.system.mapper.SysJobMapper;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 定时任务通用代理（Quartz 侧唯一注册的 Job 类）
 *
 * <p>任务定义存 {@code sys_job}，通过 {@code JobDataMap} 携带 {@code jobId}；
 * 触发时回查任务 → 任务级并发锁控制 → 同步写「运行中」执行记录 →
 * 按 {@code invokeTarget}（beanName.method()）反射调用业务方法 → 异步更新记录结果。
 *
 * <p>业务侧新增任务无需编写任何 Quartz Job 类，仅需一个普通 {@code @Component} Bean 方法。
 *
 * @author zhush
 */
@Slf4j
@Component
public class SysJobInvokeJob extends AbstractJob {

    /**
     * 任务级并发锁：禁止并发（concurrent=0）的任务，运行中再次触发直接跳过
     */
    private static final Map<Long, AtomicBoolean> RUNNING_LOCKS = new ConcurrentHashMap<>();

    @Autowired
    private SysJobMapper jobMapper;

    @Autowired
    private JobLogService jobLogService;

    @Autowired
    private JobInvokeTargetParser invokeTargetParser;

    @Override
    protected void doExecute(JobExecutionContext context) throws Exception {
        Long jobId = context.getMergedJobDataMap().getLong(JobInvokeTargetParser.JOB_ID_KEY);
        SysJob job = jobMapper.selectOneById(jobId);
        if (job == null) {
            // 任务已被删除但调度器残留触发（删除补偿失败等极端场景），记录告警后终止
            log.warn("[SysJob] 触发的任务不存在，跳过执行，jobId={}", jobId);
            return;
        }

        AtomicBoolean running = RUNNING_LOCKS.computeIfAbsent(jobId, id -> new AtomicBoolean(false));
        boolean forbidConcurrent = job.getConcurrent() != null && job.getConcurrent() == 0;
        if (forbidConcurrent && !running.compareAndSet(false, true)) {
            log.warn("[SysJob] 任务正在运行中（禁止并发），跳过本次触发，jobId={}", jobId);
            jobLogService.skippedLog(job);
            return;
        }

        SysJobLog jobLog = jobLogService.startLog(job);
        long startMs = System.currentTimeMillis();
        try {
            Object result = invokeTargetParser.invoke(job);
            jobLogService.successLog(jobLog.getId(), startMs, result);
        } catch (Exception e) {
            jobLogService.failLog(jobLog.getId(), startMs, e);
            // 上抛由 AbstractJob 包装为 JobExecutionException，不影响调度器运行
            throw e;
        } finally {
            running.set(false);
        }
    }
}
