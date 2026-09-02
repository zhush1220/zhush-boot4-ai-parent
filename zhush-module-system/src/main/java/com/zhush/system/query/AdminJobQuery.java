package com.zhush.system.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 定时任务分页查询请求
 *
 * @author zhush
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "定时任务分页查询请求")
public class AdminJobQuery extends BaseQuery {

    @Schema(description = "任务名称（模糊匹配）")
    private String jobName;

    @Schema(description = "任务分组（精确匹配）")
    private String jobGroup;

    @Schema(description = "状态：0-停用 1-启用")
    private Integer status;
}
