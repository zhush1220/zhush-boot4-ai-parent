package com.zhush.system.job;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.system.entity.SysJob;
import com.zhush.system.entity.SysJobLog;
import com.zhush.system.enums.SystemErrorCode;
import com.zhush.system.mapper.SysJobLogMapper;
import com.zhush.system.query.AdminJobLogQuery;
import com.zhush.system.vo.AdminJobLogVO;
import io.github.linpeilie.Converter;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * 定时任务执行记录服务
 *
 * <p>写入链路：任务开始执行时同步插入「运行中」记录，结束后由独立线程池异步更新为
 * 成功/失败（含耗时与异常堆栈），不阻塞调度线程；管理端提供分页查询、详情与删除。
 *
 * @author zhush
 */
@Slf4j
@Service
public class JobLogService {

    /**
     * 执行消息最大长度（与 job_message 字段一致）
     */
    private static final int MAX_MESSAGE_LENGTH = 2000;

    /**
     * 异常堆栈最大长度（防止超大堆栈拖慢写入）
     */
    private static final int MAX_STACKTRACE_LENGTH = 4000;

    /**
     * 异步落库线程池：单线程顺序写入，队列打满时丢弃并告警（日志不能拖垮调度）
     */
    private final ExecutorService logExecutor = new ThreadPoolExecutor(
            1, 1, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(8192),
            runnable -> {
                Thread thread = new Thread(runnable, "job-log-writer");
                thread.setDaemon(true);
                return thread;
            },
            (runnable, executor) -> log.warn("[JobLog] 执行记录队列已满，丢弃一条更新"));

    private final SysJobLogMapper jobLogMapper;
    private final Converter converter;

    public JobLogService(SysJobLogMapper jobLogMapper, Converter converter) {
        this.jobLogMapper = jobLogMapper;
        this.converter = converter;
    }

    // ================ 写入链路（调度侧） ================

    /**
     * 任务开始执行：同步插入「运行中」记录（一次 insert 开销极小，进程崩溃时可保留现场）
     */
    public SysJobLog startLog(SysJob job) {
        SysJobLog jobLog = new SysJobLog();
        jobLog.setJobId(job.getId());
        jobLog.setJobName(job.getJobName());
        jobLog.setJobGroup(job.getJobGroup());
        jobLog.setInvokeTarget(job.getInvokeTarget());
        jobLog.setStatus(SysJobLog.STATUS_RUNNING);
        jobLog.setJobMessage("任务开始执行");
        jobLog.setStartTime(LocalDateTime.now());
        jobLogMapper.insert(jobLog);
        return jobLog;
    }

    /**
     * 任务执行成功：异步更新记录
     */
    public void successLog(Long logId, long startMs, Object result) {
        String message = result == null ? "执行成功" : "执行成功，返回: " + result;
        updateLogAsync(logId, SysJobLog.STATUS_SUCCESS, message, null, startMs);
    }

    /**
     * 任务执行失败：异步更新记录（含异常堆栈）
     */
    public void failLog(Long logId, long startMs, Throwable throwable) {
        updateLogAsync(logId, SysJobLog.STATUS_FAIL,
                "执行失败: " + throwable.getMessage(), stackTraceOf(throwable), startMs);
    }

    /**
     * 任务跳过执行（禁止并发时已在运行）：异步写入一条失败记录
     */
    public void skippedLog(SysJob job) {
        SysJobLog jobLog = new SysJobLog();
        jobLog.setJobId(job.getId());
        jobLog.setJobName(job.getJobName());
        jobLog.setJobGroup(job.getJobGroup());
        jobLog.setInvokeTarget(job.getInvokeTarget());
        jobLog.setStatus(SysJobLog.STATUS_FAIL);
        jobLog.setJobMessage("跳过执行：任务正在运行中（禁止并发）");
        LocalDateTime now = LocalDateTime.now();
        jobLog.setStartTime(now);
        jobLog.setEndTime(now);
        jobLog.setDuration(0L);
        logExecutor.execute(() -> {
            try {
                jobLogMapper.insert(jobLog);
            } catch (Exception e) {
                log.error("[JobLog] 跳过记录落库失败, jobId={}", job.getId(), e);
            }
        });
    }

    // ================ 管理端查询 ================

    /**
     * 执行记录分页
     */
    public PageResult<AdminJobLogVO> getJobLogPage(AdminJobLogQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(SysJobLog::getJobId, query.getJobId())
                .like(SysJobLog::getJobName, query.getJobName())
                .eq(SysJobLog::getStatus, query.getStatus())
                .ge(SysJobLog::getStartTime, query.getStartTimeBegin())
                .le(SysJobLog::getStartTime, query.getStartTimeEnd());
        queryWrapper.orderBy("id", false);
        Page<SysJobLog> page = jobLogMapper.selectPageX(query, queryWrapper);
        List<AdminJobLogVO> list = converter.convert(page.getRecords(), AdminJobLogVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

    /**
     * 执行记录详情
     */
    public AdminJobLogVO getJobLog(Long id) {
        SysJobLog jobLog = jobLogMapper.selectOneById(id);
        assertNotNull(jobLog, SystemErrorCode.JOB_LOG_NOT_FOUND, id);
        return converter.convert(jobLog, AdminJobLogVO.class);
    }

    /**
     * 删除执行记录（逻辑删除）
     */
    public void deleteJobLog(Long id) {
        SysJobLog jobLog = jobLogMapper.selectOneById(id);
        assertNotNull(jobLog, SystemErrorCode.JOB_LOG_NOT_FOUND, id);
        jobLogMapper.deleteById(id);
    }

    // ================ 内部方法 ================

    private void updateLogAsync(Long logId, int status, String message,
                                String exceptionInfo, long startMs) {
        SysJobLog update = new SysJobLog();
        update.setId(logId);
        update.setStatus(status);
        update.setJobMessage(StrUtil.maxLength(message, MAX_MESSAGE_LENGTH));
        update.setExceptionInfo(exceptionInfo);
        update.setEndTime(LocalDateTime.now());
        update.setDuration(System.currentTimeMillis() - startMs);
        logExecutor.execute(() -> {
            try {
                jobLogMapper.update(update);
            } catch (Exception e) {
                log.error("[JobLog] 执行记录更新失败, logId={}", logId, e);
            }
        });
    }

    private String stackTraceOf(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return StrUtil.maxLength(writer.toString(), MAX_STACKTRACE_LENGTH);
    }

    /**
     * 应用关闭时等待队列中的记录写完
     */
    @PreDestroy
    public void shutdown() {
        logExecutor.shutdown();
        try {
            if (!logExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                logExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
