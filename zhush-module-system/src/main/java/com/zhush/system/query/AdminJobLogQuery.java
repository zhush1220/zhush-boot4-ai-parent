package com.zhush.system.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 定时任务执行记录分页查询请求
 *
 * @author zhush
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "定时任务执行记录分页查询请求")
public class AdminJobLogQuery extends BaseQuery {

    @Schema(description = "任务ID（精确匹配）")
    private Long jobId;

    @Schema(description = "任务名称（模糊匹配）")
    private String jobName;

    @Schema(description = "执行状态：0-失败 1-成功 2-运行中")
    private Integer status;

    @Schema(description = "开始时间-起")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTimeBegin;

    @Schema(description = "开始时间-止")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTimeEnd;
}
