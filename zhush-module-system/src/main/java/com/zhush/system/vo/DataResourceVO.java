package com.zhush.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据资源详情 / 列表返回
 *
 * @author zhush
 */
@Data
@Schema(description = "数据资源详情 / 列表返回")
public class DataResourceVO {

    @Schema(description = "资源ID")
    private Long id;

    @Schema(description = "资源编码")
    private String code;

    @Schema(description = "资源名称")
    private String name;

    @Schema(description = "所属模块")
    private String module;

    @Schema(description = "业务表名")
    private String tableName;

    @Schema(description = "部门归属列名")
    private String deptColumn;

    @Schema(description = "用户归属列名")
    private String userColumn;

    @Schema(description = "自定义条件列白名单（逗号分隔）")
    private String filterColumns;

    @Schema(description = "是否纳入行级管控：0-否 1-是")
    private Integer rowEnabled;

    @Schema(description = "是否纳入列级管控：0-否 1-是")
    private Integer fieldEnabled;

    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
