package com.zhush.member.controller.app;

import com.zhush.common.response.Result;
import com.zhush.member.param.MemberLoginParam;
import com.zhush.member.param.MemberWxMiniLoginParam;
import com.zhush.member.service.MemberAuthService;
import com.zhush.member.vo.MemberInfoVO;
import com.zhush.member.vo.MemberLoginVO;
import com.zhush.satoken.annotation.IgnoreAuth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员端认证接口
 *
 * @author zhush
 */
@RestController
@Tag(name = "会员端-认证", description = "微信小程序登录、账号登录、登出、当前会员信息")
@RequestMapping("/member/auth")
public class MemberAuthController {

    private final MemberAuthService authService;

    public MemberAuthController(MemberAuthService authService) {
        this.authService = authService;
    }

    /**
     * 微信小程序登录（静默注册）
     */
    @IgnoreAuth
    @PostMapping("/wx-mini-login")
    @Operation(summary = "微信小程序登录（静默注册）")
    public Result<MemberLoginVO> wxMiniLogin(@RequestBody @Valid MemberWxMiniLoginParam param,
                                             HttpServletRequest request) {
        return Result.success(authService.wxMiniLogin(param, request.getRemoteAddr()));
    }

    /**
     * 账号密码登录
     */
    @IgnoreAuth
    @PostMapping("/login")
    @Operation(summary = "账号密码登录")
    public Result<MemberLoginVO> login(@RequestBody @Valid MemberLoginParam param,
                                       HttpServletRequest request) {
        return Result.success(authService.passwordLogin(param, request.getRemoteAddr()));
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
     * 当前登录会员信息
     */
    @GetMapping("/user-info")
    @Operation(summary = "当前登录会员信息")
    public Result<MemberInfoVO> userInfo() {
        return Result.success(authService.getUserInfo());
    }

}
