package com.zhush.system.param;

import com.zhush.system.entity.SysPost;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 岗位新增/修改请求参数
 *
 * @author zhush
 */
@Data
@AutoMapper(target = SysPost.class)
@Schema(description = "岗位新增/修改请求参数")
public class AdminPostParam {

    @Schema(description = "岗位ID（新增时不需要传）")
    private Long id;

    @Schema(description = "岗位编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "岗位编码不能为空")
    private String postCode;

    @Schema(description = "岗位名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "岗位名称不能为空")
    private String postName;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "状态：0-禁用 1-启用（不传默认启用）")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
