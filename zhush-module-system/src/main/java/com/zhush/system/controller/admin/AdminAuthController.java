package com.zhush.system.controller.admin;

import com.zhush.captcha.core.CaptchaService;
import com.zhush.captcha.vo.CaptchaVO;
import com.zhush.common.response.Result;
import com.zhush.satoken.annotation.IgnoreAuth;
import com.zhush.system.param.AdminLoginParam;
import com.zhush.system.param.AdminSocialLoginParam;
import com.zhush.system.service.AdminAuthService;
import com.zhush.system.vo.AdminLoginVO;
import com.zhush.system.vo.AdminUserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
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
@Tag(name = "管理端-认证", description = "管理端登录、登出、当前登录用户信息")
@RequestMapping("/admin/auth")
public class AdminAuthController {

    private final AdminAuthService authService;
    private final CaptchaService captchaService;

    public AdminAuthController(AdminAuthService authService,
                               ObjectProvider<CaptchaService> captchaServiceProvider) {
        this.authService = authService;
        this.captchaService = captchaServiceProvider.getIfAvailable();
    }

    /**
     * 获取登录验证码（验证码关闭时返回 null，前端据此跳过验证码流程）
     */
    @IgnoreAuth
    @GetMapping("/captcha")
    @Operation(summary = "获取登录验证码")
    public Result<CaptchaVO> captcha() {
        return Result.success(captchaService == null ? null : captchaService.generate());
    }

    /**
     * 账号密码登录
     */
    @IgnoreAuth
    @PostMapping("/login")
    @Operation(summary = "账号密码登录")
    public Result<AdminLoginVO> login(@RequestBody @Valid AdminLoginParam param,
                                      HttpServletRequest request) {
        return Result.success(authService.login(param, request.getRemoteAddr(), request.getHeader("User-Agent")));
    }

    /**
     * 第三方登录（预留：企业微信等）
     */
    @IgnoreAuth
    @PostMapping("/social-login")
    @Operation(summary = "第三方登录（预留：企业微信等）")
    public Result<AdminLoginVO> socialLogin(@RequestBody @Valid AdminSocialLoginParam loginParam,
                                                HttpServletRequest request) {
        return Result.success(authService.socialLogin(loginParam, request.getRemoteAddr()));
    }

    /**
     * 注销登录
     */
    @PostMapping("/logout")
    @Operation(summary = "注销登录")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    /**
     * 当前登录用户信息
     */
    @GetMapping("/user-info")
    @Operation(summary = "当前登录用户信息")
    public Result<AdminUserInfoVO> userInfo() {
        return Result.success(authService.getUserInfo());
    }

}
