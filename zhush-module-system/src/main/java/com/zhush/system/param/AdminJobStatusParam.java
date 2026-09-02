package com.zhush.system.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 定时任务启动/停止请求参数
 *
 * @author zhush
 */
@Data
@Schema(description = "定时任务启动/停止请求参数")
public class AdminJobStatusParam {

    @Schema(description = "任务ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "任务ID不能为空")
    private Long id;

    @Schema(description = "状态：0-停止 1-启动", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    private Integer status;
}
