package com.zhush.system.controller.admin;

import com.zhush.captcha.core.CaptchaService;
import com.zhush.captcha.vo.CaptchaVO;
import com.zhush.common.exception.GlobalErrorCode;
import com.zhush.common.response.Result;
import com.zhush.satoken.annotation.IgnoreAuth;
import com.zhush.system.param.AdminLoginParam;
import com.zhush.system.param.AdminSocialLoginParam;
import com.zhush.system.service.AdminAuthService;
import com.zhush.system.service.RefreshTokenService;
import com.zhush.system.vo.AdminLoginVO;
import com.zhush.system.vo.AdminUserInfoVO;
import com.zhush.system.vo.AdminUserMenuVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    private final RefreshTokenService refreshTokenService;
    private final CaptchaService captchaService;

    public AdminAuthController(AdminAuthService authService,
                               RefreshTokenService refreshTokenService,
                               ObjectProvider<CaptchaService> captchaServiceProvider) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
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
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        AdminLoginVO loginVO = authService.login(param, request.getRemoteAddr(), request.getHeader("User-Agent"));
        // refresh-token 经 HttpOnly Cookie 下发（浏览器端）；非浏览器端从响应体自行保存
        refreshTokenService.writeCookie(response, loginVO.getRefreshToken());
        return Result.success(loginVO);
    }

    /**
     * 刷新令牌（access-token 过期后无感续期）
     *
     * <p>refresh-token 双通道读取：Cookie 优先，其次 X-Refresh-Token 请求头 / refreshToken 参数（兼容小程序）。
     * 成功返回新凭证并轮换 Cookie；失败返回 HTTP 401，前端跳登录页。
     */
    @IgnoreAuth
    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌（换取新 access-token）")
    public ResponseEntity<Result<AdminLoginVO>> refresh(HttpServletRequest request,
                                                        HttpServletResponse response) {
        String refreshToken = refreshTokenService.resolveFromRequest(request);
        AdminLoginVO loginVO = authService.refresh(refreshToken);
        if (loginVO == null) {
            refreshTokenService.clearCookie(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Result.error(GlobalErrorCode.UNAUTHORIZED));
        }
        refreshTokenService.writeCookie(response, loginVO.getRefreshToken());
        return ResponseEntity.ok(Result.success(loginVO));
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
    public Result<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        // 双端清理：作废 refresh-token（含 Cookie 清除）+ 注销当前 access-token 会话
        authService.logout(refreshTokenService.resolveFromRequest(request));
        refreshTokenService.clearCookie(response);
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

    /**
     * 当前登录用户的菜单路由树（登录后前端动态生成路由/菜单）
     */
    @GetMapping("/menus")
    @Operation(summary = "当前用户菜单路由树")
    public Result<List<AdminUserMenuVO>> menus() {
        return Result.success(authService.getUserMenus());
    }

    /**
     * 当前登录用户的权限标识列表（前端按钮级鉴权）
     */
    @GetMapping("/codes")
    @Operation(summary = "当前用户权限标识列表")
    public Result<List<String>> codes() {
        return Result.success(authService.getAccessCodes());
    }

}
