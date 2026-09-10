package com.zhush.system.service;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.zhush.common.exception.GlobalErrorCode;
import com.zhush.common.response.PageResult;
import com.zhush.job.core.JobUtils;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.system.entity.SysJob;
import com.zhush.system.enums.SystemErrorCode;
import com.zhush.system.job.JobInvokeTargetParser;
import com.zhush.system.job.SysJobInvokeJob;
import com.zhush.system.mapper.SysJobMapper;
import com.zhush.system.param.AdminJobParam;
import com.zhush.system.query.AdminJobQuery;
import com.zhush.system.vo.AdminJobVO;
import io.github.linpeilie.Converter;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronExpression;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;
import static com.zhush.common.exception.ServiceExceptionUtil.assertTrue;

/**
 * 定时任务管理服务
 *
 * <p>任务定义存 {@code sys_job}，Quartz 侧统一注册通用代理 {@link SysJobInvokeJob}
 * （JobDataMap 携带任务ID）；增删改查/启停/手动触发均遵循「先校验，再落库，
 * 最后同步调度器，调度失败反向补偿数据库」的顺序，保证两侧最终一致。
 *
 * @author zhush
 */
@Slf4j
@Service
public class AdminJobService {

    private static final int STATUS_ENABLED = 1;
    private static final int STATUS_DISABLED = 0;
    private static final int CONCURRENT_ALLOW = 1;
    private static final String DEFAULT_JOB_GROUP = "DEFAULT";

    private final SysJobMapper jobMapper;
    private final ObjectProvider<JobUtils> jobUtilsProvider;
    private final JobInvokeTargetParser invokeTargetParser;
    private final Converter converter;

    public AdminJobService(SysJobMapper jobMapper,
                           ObjectProvider<JobUtils> jobUtilsProvider,
                           JobInvokeTargetParser invokeTargetParser,
                           Converter converter) {
        this.jobMapper = jobMapper;
        this.jobUtilsProvider = jobUtilsProvider;
        this.invokeTargetParser = invokeTargetParser;
        this.converter = converter;
    }

    // ================ 任务管理 ================

    /**
     * 新增定时任务（启用状态时立即注册调度）
     */
    public Long createJob(AdminJobParam param) {
        validateBasic(param);
        String jobGroup = realGroup(param.getJobGroup());
        validateNameUnique(null, param.getJobName(), jobGroup);

        SysJob job = converter.convert(param, SysJob.class);
        job.setJobGroup(jobGroup);
        if (job.getConcurrent() == null) {
            job.setConcurrent(CONCURRENT_ALLOW);
        }
        if (job.getStatus() == null) {
            job.setStatus(STATUS_ENABLED);
        }
        jobMapper.insert(job);
        if (STATUS_ENABLED == job.getStatus()) {
            try {
                scheduleJob(job);
            } catch (Exception e) {
                // 补偿：调度注册失败时删除刚落库的任务定义
                jobMapper.deleteById(job.getId());
                throw e;
            }
        }
        return job.getId();
    }

    /**
     * 修改定时任务（名称/分组变更 = 删除旧调度键 + 重建新键；启用任务覆盖重建调度配置）
     */
    public void updateJob(AdminJobParam param) {
        SysJob existing = getRequired(param.getId());
        validateBasic(param);
        String newGroup = realGroup(param.getJobGroup());
        validateNameUnique(existing.getId(), param.getJobName(), newGroup);

        SysJob update = converter.convert(param, SysJob.class);
        update.setJobGroup(newGroup);
        jobMapper.update(update);
        SysJob latest = getRequired(existing.getId());
        try {
            JobUtils jobUtils = requireJobUtils();
            boolean keyChanged = !StrUtil.equals(existing.getJobName(), latest.getJobName())
                    || !StrUtil.equals(existing.getJobGroup(), latest.getJobGroup());
            if (keyChanged) {
                jobUtils.deleteJob(existing.getJobName(), existing.getJobGroup());
            }
            if (STATUS_ENABLED == latest.getStatus()) {
                // addCronJob 自带删除重建语义，覆盖旧 Cron 与参数
                scheduleJob(latest);
            } else if (jobUtils.exists(latest.getJobName(), latest.getJobGroup())) {
                // 停用任务：清理调度器残留
                jobUtils.deleteJob(latest.getJobName(), latest.getJobGroup());
            }
        } catch (Exception e) {
            // 补偿：恢复数据库旧值 + 重新注册旧调度
            rollbackUpdate(existing);
            throw e;
        }
    }

    /**
     * 删除定时任务（先移除调度再逻辑删除，调度失败数据库不变）
     */
    public void deleteJob(Long id) {
        SysJob job = getRequired(id);
        requireJobUtils().deleteJob(job.getJobName(), job.getJobGroup());
        jobMapper.deleteById(id);
    }

    /**
     * 批量删除定时任务（循环复用单条删除的校验与调度清理）
     */
    public void deleteJobBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        ids.forEach(this::deleteJob);
    }

    /**
     * 启动 / 停止任务（幂等：状态未变化直接返回）
     */
    public void changeStatus(Long id, Integer status) {
        SysJob job = getRequired(id);
        assertTrue(STATUS_DISABLED == status || STATUS_ENABLED == status,
                GlobalErrorCode.BAD_REQUEST);
        if (status.equals(job.getStatus())) {
            return;
        }
        SysJob update = new SysJob();
        update.setId(id);
        update.setStatus(status);
        jobMapper.update(update);
        try {
            JobUtils jobUtils = requireJobUtils();
            if (STATUS_ENABLED == status) {
                if (jobUtils.exists(job.getJobName(), job.getJobGroup())) {
                    jobUtils.resumeJob(job.getJobName(), job.getJobGroup());
                } else {
                    scheduleJob(job);
                }
            } else if (jobUtils.exists(job.getJobName(), job.getJobGroup())) {
                jobUtils.pauseJob(job.getJobName(), job.getJobGroup());
            }
        } catch (Exception e) {
            // 补偿：恢复数据库原状态
            SysJob rollback = new SysJob();
            rollback.setId(id);
            rollback.setStatus(job.getStatus());
            jobMapper.update(rollback);
            throw e;
        }
    }

    /**
     * 立即执行一次（不等待 Cron 周期，不影响原调度；未注册时先补注册）
     */
    public void triggerJob(Long id) {
        SysJob job = getRequired(id);
        JobUtils jobUtils = requireJobUtils();
        if (!jobUtils.exists(job.getJobName(), job.getJobGroup())) {
            scheduleJob(job);
        }
        jobUtils.triggerOnce(job.getJobName(), job.getJobGroup());
        if (STATUS_DISABLED == job.getStatus()) {
            // 停用任务补注册后立即暂停，避免 Cron 周期意外生效
            jobUtils.pauseJob(job.getJobName(), job.getJobGroup());
        }
    }

    /**
     * 任务详情（含下次执行时间）
     */
    public AdminJobVO getJob(Long id) {
        SysJob job = getRequired(id);
        AdminJobVO vo = converter.convert(job, AdminJobVO.class);
        fillNextTime(vo);
        return vo;
    }

    /**
     * 任务分页（启用中任务附下次执行时间）
     */
    public PageResult<AdminJobVO> getJobPage(AdminJobQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(SysJob::getJobName, query.getJobName())
                .eq(SysJob::getJobGroup, query.getJobGroup())
                .eq(SysJob::getStatus, query.getStatus());
        queryWrapper.orderBy("id", false);
        Page<SysJob> page = jobMapper.selectPageX(query, queryWrapper);
        List<AdminJobVO> list = converter.convert(page.getRecords(), AdminJobVO.class);
        list.forEach(this::fillNextTime);
        return PageResult.of(list, page.getTotalRow());
    }

    // ================ 启动补漏注册 ================

    /**
     * 应用启动时为启用任务补漏注册调度（已存在则跳过；单个任务失败不影响其他任务）
     */
    public void syncEnabledJobsOnStartup() {
        JobUtils jobUtils = jobUtilsProvider.getIfAvailable();
        if (jobUtils == null) {
            log.warn("[SysJob] 定时任务总开关关闭（zhush.job.enabled=false），跳过启动同步");
            return;
        }
        List<SysJob> jobs = jobMapper.selectListByQuery(QueryWrapperX.create()
                .eq(SysJob::getStatus, STATUS_ENABLED));
        for (SysJob job : jobs) {
            try {
                if (!jobUtils.exists(job.getJobName(), job.getJobGroup())) {
                    scheduleJob(job);
                    log.info("[SysJob] 启动补漏注册任务: {}({})", job.getJobName(), job.getJobGroup());
                }
            } catch (Exception e) {
                log.error("[SysJob] 启动补漏注册失败, jobId={}", job.getId(), e);
            }
        }
    }

    // ================ 内部方法 ================

    /**
     * 注册/覆盖调度（JobDataMap 仅携带任务ID，执行时回查任务定义）
     */
    private void scheduleJob(SysJob job) {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put(JobInvokeTargetParser.JOB_ID_KEY, job.getId());
        requireJobUtils().addCronJob(SysJobInvokeJob.class, job.getJobName(),
                job.getJobGroup(), job.getCronExpression(), dataMap);
    }

    private SysJob getRequired(Long id) {
        SysJob job = jobMapper.selectOneById(id);
        assertNotNull(job, SystemErrorCode.JOB_NOT_FOUND, id);
        return job;
    }

    private JobUtils requireJobUtils() {
        JobUtils jobUtils = jobUtilsProvider.getIfAvailable();
        assertNotNull(jobUtils, SystemErrorCode.JOB_SCHEDULER_DISABLED);
        return jobUtils;
    }

    /**
     * Cron 合法性 + 调用目标合法性（Bean/方法存在、参数为合法 JSON 对象）
     */
    private void validateBasic(AdminJobParam param) {
        assertTrue(CronExpression.isValidExpression(param.getCronExpression()),
                SystemErrorCode.JOB_CRON_INVALID, param.getCronExpression());
        invokeTargetParser.validate(param.getInvokeTarget(), param.getParams());
    }

    private void validateNameUnique(Long excludeId, String jobName, String jobGroup) {
        SysJob existing = jobMapper.selectByJobNameAndGroup(jobName, jobGroup);
        assertTrue(existing == null || existing.getId().equals(excludeId),
                SystemErrorCode.JOB_NAME_DUPLICATE, jobName);
    }

    /**
     * 修改失败补偿：恢复数据库旧值并重新注册旧调度
     */
    private void rollbackUpdate(SysJob existing) {
        try {
            SysJob rollback = new SysJob();
            rollback.setId(existing.getId());
            rollback.setJobName(existing.getJobName());
            rollback.setJobGroup(existing.getJobGroup());
            rollback.setInvokeTarget(existing.getInvokeTarget());
            rollback.setCronExpression(existing.getCronExpression());
            rollback.setParams(existing.getParams());
            rollback.setConcurrent(existing.getConcurrent());
            rollback.setStatus(existing.getStatus());
            rollback.setRemark(existing.getRemark());
            jobMapper.update(rollback);
            if (STATUS_ENABLED == existing.getStatus()) {
                scheduleJob(existing);
            }
        } catch (Exception ex) {
            log.error("[SysJob] 修改任务补偿失败, jobId={}", existing.getId(), ex);
        }
    }

    private String realGroup(String jobGroup) {
        return StrUtil.isBlank(jobGroup) ? DEFAULT_JOB_GROUP : jobGroup;
    }

    /**
     * 填充下次执行时间（仅启用任务；调度开关关闭或任务未注册时为空）
     */
    private void fillNextTime(AdminJobVO vo) {
        if (vo.getStatus() == null || STATUS_ENABLED != vo.getStatus()) {
            return;
        }
        JobUtils jobUtils = jobUtilsProvider.getIfAvailable();
        if (jobUtils == null) {
            return;
        }
        Date next = jobUtils.getNextFireTime(vo.getJobName(), vo.getJobGroup());
        if (next != null) {
            vo.setNextTime(next.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
    }
}
