package com.zhush.system.param;

import com.zhush.system.entity.SysJob;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 定时任务新增/修改请求参数
 *
 * @author zhush
 */
@Data
@AutoMapper(target = SysJob.class)
@Schema(description = "定时任务新增/修改请求参数")
public class AdminJobParam {

    @Schema(description = "任务ID（新增时不需要传）")
    private Long id;

    @Schema(description = "任务名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "任务名称不能为空")
    private String jobName;

    @Schema(description = "任务分组（不传默认 DEFAULT）")
    private String jobGroup;

    @Schema(description = "调用目标：Spring Bean 方法，如 orderService.closeExpiredOrders()",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "调用目标不能为空")
    private String invokeTarget;

    @Schema(description = "Cron 表达式（Quartz 7 段格式）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Cron 表达式不能为空")
    private String cronExpression;

    @Schema(description = "任务参数（JSON，透传给调用目标方法）")
    private String params;

    @Schema(description = "是否允许并发执行：0-禁止 1-允许（不传默认允许）")
    private Integer concurrent;

    @Schema(description = "状态：0-停用 1-启用（不传默认启用）")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
