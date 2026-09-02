package com.zhush.system.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 定时任务
 *
 * <p>任务定义存库，Quartz 侧通过通用代理 {@code SysJobInvokeJob} 按
 * {@code invokeTarget}（beanName.method()）反射调用 Spring Bean 方法。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_job")
public class SysJob extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 任务名称 */
    private String jobName;

    /** 任务分组 */
    private String jobGroup;

    /** 调用目标：Spring Bean 方法，如 orderService.closeExpiredOrders() */
    private String invokeTarget;

    /** Cron 表达式（Quartz 7 段格式） */
    private String cronExpression;

    /** 任务参数（JSON，透传给调用目标方法） */
    private String params;

    /** 是否允许并发执行：0-禁止 1-允许 */
    private Integer concurrent;

    /** 状态：0-停用 1-启用（即启动/停止） */
    private Integer status;

    /** 备注 */
    private String remark;
}
