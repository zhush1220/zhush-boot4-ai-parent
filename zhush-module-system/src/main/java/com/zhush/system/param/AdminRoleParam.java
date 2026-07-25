package com.zhush.system.param;

import com.zhush.system.entity.SysRole;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * @ClassName AdminRoleParam
 * @Description TODO
 * @Author zhush
 * @Date 2026/7/20 0020 22:30
 **/
@Data
@AutoMapper(target = SysRole.class)
@Schema(description = "角色新增/修改请求参数")
public class AdminRoleParam {

    @Schema(description = "角色ID（新增时不需要传）")
    private Long id;

    @Schema(description = "角色名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "角色名称不能为空")
    private String name;

    @Schema(description = "角色编码（如 admin、ops）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "角色编码不能为空")
    private String code;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "数据权限：1-全部 2-本部门 3-本部门及以下 4-仅本人 5-自定义")
    private Integer dataScope;

    @Schema(description = "状态：0-禁用 1-启用（不传默认启用）")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "菜单ID列表（授予的权限）")
    private List<Long> menuIds;

}
