package com.zhush.system.param;

import com.zhush.system.entity.SysDictType;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 字典类型新增/修改请求参数
 *
 * @author zhush
 */
@Data
@AutoMapper(target = SysDictType.class)
@Schema(description = "字典类型新增/修改请求参数")
public class AdminDictTypeParam {

    @Schema(description = "字典类型ID（新增时不需要传）")
    private Long id;

    @Schema(description = "字典名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "字典名称不能为空")
    private String name;

    @Schema(description = "字典类型编码（如 common_status）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "字典类型编码不能为空")
    private String type;

    @Schema(description = "状态：0-禁用 1-启用（不传默认启用）")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
