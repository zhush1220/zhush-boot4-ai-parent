package com.zhush.system.vo;

import com.zhush.system.entity.SysJobLog;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定时任务执行记录详情 / 列表返回
 *
 * @author zhush
 */
@Data
@AutoMapper(target = SysJobLog.class)
@Schema(description = "定时任务执行记录详情 / 列表返回")
public class AdminJobLogVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "任务ID")
    private Long jobId;

    @Schema(description = "任务名称")
    private String jobName;

    @Schema(description = "任务分组")
    private String jobGroup;

    @Schema(description = "调用目标")
    private String invokeTarget;

    @Schema(description = "执行状态：0-失败 1-成功 2-运行中")
    private Integer status;

    @Schema(description = "执行消息（成功摘要 / 失败原因）")
    private String jobMessage;

    @Schema(description = "异常堆栈（失败时返回）")
    private String exceptionInfo;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "耗时（毫秒）")
    private Long duration;

    @Schema(description = "记录时间")
    private LocalDateTime createTime;
}
