package com.zhush.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 受控字段（列级数据权限可授权字段）返回
 *
 * @author zhush
 */
@Data
@Schema(description = "受控字段返回")
public class DataFieldVO {

    @Schema(description = "字段ID")
    private Long id;

    @Schema(description = "所属资源ID")
    private Long resourceId;

    @Schema(description = "VO 字段路径")
    private String fieldPath;

    @Schema(description = "字段中文名")
    private String label;

    @Schema(description = "权限标识（授权即分配该 sys_menu 按钮）")
    private String permissionCode;

    @Schema(description = "关联的 sys_menu 按钮ID")
    private Long menuId;

    @Schema(description = "默认策略：CLEAR-置空 MASK-脱敏")
    private String defaultStrategy;

    @Schema(description = "脱敏占位值")
    private String maskValue;

    @Schema(description = "是否纳入列级管控：0-否 1-是")
    private Integer status;

}
