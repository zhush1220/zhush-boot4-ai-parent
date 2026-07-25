package com.zhush.system.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @ClassName AdminSocialLoginParam
 * @Description 管理端第三方登录请求（预留：企业微信、钉钉等）
 * @Author zhush
 * @Date 2026/7/20 0020 22:01
 **/
@Data
@Schema(description = "管理端第三方登录请求（预留：企业微信、钉钉等）")
public class AdminSocialLoginParam {

    @Schema(description = "第三方类型：WECOM-企业微信 DINGTALK-钉钉 ...", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "第三方类型不能为空")
    private String socialType;

    @Schema(description = "第三方授权码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "授权码不能为空")
    private String code;

    @Schema(description = "登录设备：PC / APP / MINI（不传默认 PC）")
    private String device;

}
