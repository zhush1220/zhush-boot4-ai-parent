package com.zhush.system.param;

import com.zhush.system.entity.SysDictData;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 字典数据新增/修改请求参数
 *
 * @author zhush
 */
@Data
@AutoMapper(target = SysDictData.class)
@Schema(description = "字典数据新增/修改请求参数")
public class AdminDictDataParam {

    @Schema(description = "字典数据ID（新增时不需要传）")
    private Long id;

    @Schema(description = "字典类型编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "字典类型编码不能为空")
    private String dictType;

    @Schema(description = "字典标签（展示值）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "字典标签不能为空")
    private String label;

    @Schema(description = "字典键值（存储值）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "字典键值不能为空")
    private String value;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "状态：0-禁用 1-启用（不传默认启用）")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
