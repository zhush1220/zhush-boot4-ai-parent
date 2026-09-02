package com.zhush.system.vo;

import com.zhush.system.entity.SysJob;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定时任务详情 / 列表返回
 *
 * @author zhush
 */
@Data
@AutoMapper(target = SysJob.class)
@Schema(description = "定时任务详情 / 列表返回")
public class AdminJobVO {

    @Schema(description = "任务ID")
    private Long id;

    @Schema(description = "任务名称")
    private String jobName;

    @Schema(description = "任务分组")
    private String jobGroup;

    @Schema(description = "调用目标：Spring Bean 方法")
    private String invokeTarget;

    @Schema(description = "Cron 表达式")
    private String cronExpression;

    @Schema(description = "任务参数（JSON）")
    private String params;

    @Schema(description = "是否允许并发执行：0-禁止 1-允许")
    private Integer concurrent;

    @Schema(description = "状态：0-停用 1-启用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "下次执行时间（启用中任务才有值）")
    private LocalDateTime nextTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
