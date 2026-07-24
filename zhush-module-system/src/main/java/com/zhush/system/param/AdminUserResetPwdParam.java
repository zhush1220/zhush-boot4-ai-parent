package com.zhush.system.param;

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
public class AdminUserResetPwdParam {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long id;

    /**
     * 新密码（明文，服务端 BCrypt 加密存储）
     */
    @NotBlank(message = "新密码不能为空")
    private String password;

}
