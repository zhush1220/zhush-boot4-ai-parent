package com.zhush.system.param;

import com.zhush.system.entity.SysDept;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @ClassName AdminDeptParam
 * @Description 部门新增/修改请求参数
 * @Author zhush
 * @Date 2026/7/13
 **/
@Data
@AutoMapper(target = SysDept.class)
@Schema(description = "部门新增/修改请求参数")
public class AdminDeptParam {

    @Schema(description = "部门ID（新增时不需要传）")
    private Long id;

    @Schema(description = "父部门ID（根为0，不传默认0）")
    private Long parentId;

    @Schema(description = "部门名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "部门名称不能为空")
    private String name;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "负责人用户ID")
    private Long leaderUserId;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "状态：0-禁用 1-启用（不传默认启用）")
    private Integer status;

}
