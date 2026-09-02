package com.zhush.system.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 定时任务执行记录
 *
 * <p>每次调度/手动触发执行均写入一条：开始执行同步插入「运行中」记录，
 * 结束后异步更新为成功/失败（含耗时与异常堆栈）；任务删除后记录保留
 * （名称等字段为冗余快照）。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_job_log")
public class SysJobLog extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 执行状态：失败 */
    public static final int STATUS_FAIL = 0;

    /** 执行状态：成功 */
    public static final int STATUS_SUCCESS = 1;

    /** 执行状态：运行中 */
    public static final int STATUS_RUNNING = 2;

    /** 任务ID（sys_job.id） */
    private Long jobId;

    /** 任务名称（冗余快照） */
    private String jobName;

    /** 任务分组（冗余快照） */
    private String jobGroup;

    /** 调用目标（冗余快照） */
    private String invokeTarget;

    /** 执行状态：0-失败 1-成功 2-运行中 */
    private Integer status;

    /** 执行消息（成功摘要 / 失败原因） */
    private String jobMessage;

    /** 异常堆栈（失败时记录） */
    private String exceptionInfo;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 耗时（毫秒） */
    private Long duration;
}
