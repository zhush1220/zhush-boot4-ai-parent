package com.zhush.member.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 会员微信小程序登录请求
 *
 * @author zhush
 */
@Data
@Schema(description = "会员微信小程序登录请求")
public class MemberWxMiniLoginParam {

    @Schema(description = "微信小程序登录 code（wx.login 获取）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "登录 code 不能为空")
    private String code;

    @Schema(description = "登录设备：MINI / APP（不传默认 MINI）")
    private String device;
}
