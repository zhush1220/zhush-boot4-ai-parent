package com.zhush.system.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @ClassName AdminUserResetPwdParam
 * @Description 管理员重置密码请求
 * @Author zhush
 * @Date 2026/7/20 0020 22:30
 **/
@Data
@Schema(description = "管理员重置密码请求")
public class AdminUserResetPwdParam {

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "用户ID不能为空")
    private Long id;

    @Schema(description = "新密码（明文，服务端 BCrypt 加密存储）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "新密码不能为空")
    private String password;

}
