package com.zhush.system.controller.admin.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理端账号密码登录请求
 *
 * @author zhush
 */
@Data
public class AdminLoginReqVO {

    /** 登录账号 */
    @NotBlank(message = "登录账号不能为空")
    private String username;

    /** 登录密码 */
    @NotBlank(message = "登录密码不能为空")
    private String password;

    /** 登录设备：PC / APP / MINI（不传默认 PC） */
    private String device;
}
