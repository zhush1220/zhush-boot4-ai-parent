package com.zhush.system.param;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @ClassName AdminLoginParam
 * @Description TODO
 * @Author zhush
 * @Date 2026/7/20 0020 21:26
 **/
@Data
public class AdminLoginParam {

    /**
     * 登录账号
     */
    @NotBlank(message = "登录账号不能为空")
    private String username;

    /**
     * 登录密码
     */
    @NotBlank(message = "登录密码不能为空")
    private String password;

    /**
     * 登录设备：PC / APP / MINI（不传默认 PC）
     */
    private String device;
}
