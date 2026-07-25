package com.zhush.member.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 会员账号密码登录请求
 *
 * @author zhush
 */
@Data
@Schema(description = "会员账号密码登录请求")
public class MemberLoginParam {

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "手机号不能为空")
    private String mobile;

    @Schema(description = "登录密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "登录密码不能为空")
    private String password;

    @Schema(description = "登录设备：PC / APP / MINI（不传默认 MINI）")
    private String device;
}
