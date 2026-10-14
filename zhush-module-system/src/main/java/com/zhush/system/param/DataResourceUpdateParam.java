package com.zhush.system.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 数据资源编辑请求（仅开放可运维字段，元数据列由同步器维护）
 *
 * @author zhush
 */
@Data
@Schema(description = "数据资源编辑请求")
public class DataResourceUpdateParam {

    @Schema(description = "资源ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "资源ID不能为空")
    private Long id;

    @Schema(description = "资源名称")
    private String name;

    @Schema(description = "自定义条件列白名单（逗号分隔）")
    private String filterColumns;

    @Schema(description = "是否纳入行级管控：0-否 1-是")
    private Integer rowEnabled;

    @Schema(description = "是否纳入列级管控：0-否 1-是")
    private Integer fieldEnabled;

    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;

}
