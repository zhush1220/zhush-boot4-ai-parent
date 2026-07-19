package com.zhush.member.controller.app.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 会员微信小程序登录请求
 *
 * @author zhush
 */
@Data
public class MemberWxMiniLoginReqVO {

    /** 微信小程序登录 code（wx.login 获取） */
    @NotBlank(message = "登录 code 不能为空")
    private String code;

    /** 登录设备：MINI / APP（不传默认 MINI） */
    private String device;
}
