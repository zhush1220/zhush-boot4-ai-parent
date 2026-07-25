package com.zhush.system.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @ClassName AdminLoginParam
 * @Description TODO
 * @Author zhush
 * @Date 2026/7/20 0020 21:26
 **/
@Data
@Schema(description = "管理端账号密码登录请求")
public class AdminLoginParam {

    @Schema(description = "登录账号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "登录账号不能为空")
    private String username;

    @Schema(description = "登录密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "登录密码不能为空")
    private String password;

    @Schema(description = "登录设备：PC / APP / MINI（不传默认 PC）")
    private String device;
}
