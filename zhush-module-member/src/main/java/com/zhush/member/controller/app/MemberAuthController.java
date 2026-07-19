package com.zhush.member.controller.app;

import com.zhush.common.response.Result;
import com.zhush.member.controller.app.vo.MemberInfoRespVO;
import com.zhush.member.controller.app.vo.MemberLoginReqVO;
import com.zhush.member.controller.app.vo.MemberLoginRespVO;
import com.zhush.member.controller.app.vo.MemberWxMiniLoginReqVO;
import com.zhush.member.service.MemberAuthService;
import com.zhush.satoken.annotation.IgnoreAuth;
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
    public Result<MemberLoginRespVO> wxMiniLogin(@RequestBody @Valid MemberWxMiniLoginReqVO reqVO,
                                                 HttpServletRequest request) {
        return Result.success(authService.wxMiniLogin(reqVO, request.getRemoteAddr()));
    }

    /**
     * 账号密码登录
     */
    @IgnoreAuth
    @PostMapping("/login")
    public Result<MemberLoginRespVO> login(@RequestBody @Valid MemberLoginReqVO reqVO,
                                           HttpServletRequest request) {
        return Result.success(authService.passwordLogin(reqVO, request.getRemoteAddr()));
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
     * 当前登录会员信息
     */
    @GetMapping("/user-info")
    public Result<MemberInfoRespVO> userInfo() {
        return Result.success(authService.getUserInfo());
    }

}
