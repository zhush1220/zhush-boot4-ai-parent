package com.zhush.system.controller.admin.vo.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员重置密码请求
 *
 * @author zhush
 */
@Data
public class AdminUserResetPwdReqVO {

    /** 用户ID */
    @NotNull(message = "用户ID不能为空")
    private Long id;

    /** 新密码（明文，服务端 BCrypt 加密存储） */
    @NotBlank(message = "新密码不能为空")
    private String password;
}
