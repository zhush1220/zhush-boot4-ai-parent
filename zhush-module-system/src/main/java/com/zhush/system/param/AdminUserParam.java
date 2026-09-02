package com.zhush.system.param;

import com.zhush.system.entity.SysAdminUser;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * @ClassName AdminUserParam
 * @Description 管理员新增/修改请求参数（新增时无需传 id，密码仅新增时生效）
 * @Author zhush
 * @Date 2026/7/20 0020 22:30
 **/
@Data
@AutoMapper(target = SysAdminUser.class)
@Schema(description = "管理员新增/修改请求参数（新增时无需传 id，密码仅新增时生效）")
public class AdminUserParam {

    @Schema(description = "用户ID（新增时不需要传）")
    private Long id;

    @Schema(description = "登录账号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "登录账号不能为空")
    private String username;

    @Schema(description = "登录密码（明文，服务端 BCrypt 加密存储，仅新增时生效）")
    private String password;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "性别：0-未知 1-男 2-女")
    private Integer sex;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "岗位ID")
    private Long postId;

    @Schema(description = "状态：0-禁用 1-启用（不传默认启用）")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "角色ID列表（非 null 时覆盖更新）")
    private List<Long> roleIds;

}
