package com.zhush.member.controller.app.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 会员账号密码登录请求
 *
 * @author zhush
 */
@Data
public class MemberLoginReqVO {

    /** 手机号 */
    @NotBlank(message = "手机号不能为空")
    private String mobile;

    /** 登录密码 */
    @NotBlank(message = "登录密码不能为空")
    private String password;

    /** 登录设备：PC / APP / MINI（不传默认 MINI） */
    private String device;
}
