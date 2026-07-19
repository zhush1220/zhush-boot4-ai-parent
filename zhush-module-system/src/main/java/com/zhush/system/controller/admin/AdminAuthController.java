package com.zhush.system.controller.admin;

import com.zhush.common.response.Result;
import com.zhush.satoken.annotation.IgnoreAuth;
import com.zhush.system.controller.admin.vo.AdminLoginReqVO;
import com.zhush.system.controller.admin.vo.AdminLoginRespVO;
import com.zhush.system.controller.admin.vo.AdminSocialLoginReqVO;
import com.zhush.system.controller.admin.vo.AdminUserInfoRespVO;
import com.zhush.system.service.AdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端认证接口
 *
 * @author zhush
 */
@RestController
@RequestMapping("/admin/auth")
public class AdminAuthController {

    private final AdminAuthService authService;

    public AdminAuthController(AdminAuthService authService) {
        this.authService = authService;
    }

    /**
     * 账号密码登录
     */
    @IgnoreAuth
    @PostMapping("/login")
    public Result<AdminLoginRespVO> login(@RequestBody @Valid AdminLoginReqVO reqVO,
                                          HttpServletRequest request) {
        return Result.success(authService.login(reqVO, request.getRemoteAddr()));
    }

    /**
     * 第三方登录（预留：企业微信等）
     */
    @IgnoreAuth
    @PostMapping("/social-login")
    public Result<AdminLoginRespVO> socialLogin(@RequestBody @Valid AdminSocialLoginReqVO reqVO,
                                                HttpServletRequest request) {
        return Result.success(authService.socialLogin(reqVO, request.getRemoteAddr()));
    }

    /**
     * 注销登录
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    /**
     * 当前登录用户信息
     */
    @GetMapping("/user-info")
    public Result<AdminUserInfoRespVO> userInfo() {
        return Result.success(authService.getUserInfo());
    }

}
