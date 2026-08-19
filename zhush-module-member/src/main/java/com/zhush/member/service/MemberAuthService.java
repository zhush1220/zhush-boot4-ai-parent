package com.zhush.member.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.zhush.captcha.core.CaptchaService;
import com.zhush.member.entity.MemberSocialUser;
import com.zhush.member.entity.MemberUser;
import com.zhush.member.enums.MemberErrorCode;
import com.zhush.member.mapper.MemberSocialUserMapper;
import com.zhush.member.mapper.MemberUserMapper;
import com.zhush.member.param.MemberLoginParam;
import com.zhush.member.param.MemberWxMiniLoginParam;
import com.zhush.member.social.MemberSocialTypeConstants;
import com.zhush.member.social.WxMaSession;
import com.zhush.member.social.WxMiniClient;
import com.zhush.member.vo.MemberInfoVO;
import com.zhush.member.vo.MemberLoginVO;
import com.zhush.satoken.core.DeviceTypeConstants;
import com.zhush.satoken.core.StpMemberUtil;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;
import static com.zhush.common.exception.ServiceExceptionUtil.assertTrue;

/**
 * 会员端认证服务
 *
 * <p>支持微信小程序登录（静默注册）与账号密码登录，并预留其他第三方授权扩展。
 * 会员端仅有登录态、无权限模型。多端登录同端互踢。
 *
 * @author zhush
 */
@Service
public class MemberAuthService {

    private static final int STATUS_ENABLED = 1;

    private final MemberUserMapper memberUserMapper;
    private final MemberSocialUserMapper memberSocialUserMapper;
    private final WxMiniClient wxMiniClient;
    /**
     * 验证码服务（可选：zhush.captcha.enabled=false 时不装配，登录自动跳过验证码校验）
     */
    private final CaptchaService captchaService;

    public MemberAuthService(MemberUserMapper memberUserMapper,
                             MemberSocialUserMapper memberSocialUserMapper,
                             WxMiniClient wxMiniClient,
                             ObjectProvider<CaptchaService> captchaServiceProvider) {
        this.memberUserMapper = memberUserMapper;
        this.memberSocialUserMapper = memberSocialUserMapper;
        this.wxMiniClient = wxMiniClient;
        this.captchaService = captchaServiceProvider.getIfAvailable();
    }

    /**
     * 微信小程序登录：openid 不存在则静默注册
     */
    @Transactional(rollbackFor = Exception.class)
    public MemberLoginVO wxMiniLogin(MemberWxMiniLoginParam param, String clientIp) {
        WxMaSession session = wxMiniClient.code2Session(param.getCode());

        MemberSocialUser social = memberSocialUserMapper
                .selectByTypeAndOpenId(MemberSocialTypeConstants.WECHAT_MINI, session.getOpenId());

        boolean newRegister = false;
        MemberUser user;
        if (social != null) {
            user = memberUserMapper.selectOneById(social.getUserId());
        } else {
            user = null;
        }

        if (user == null) {
            // 静默注册：新建会员 + 绑定关系
            user = registerByWxMini(session, clientIp);
            social = bindWxMiniSocial(user.getId(), session);
            newRegister = true;
        } else {
            // 刷新会话密钥 / unionId
            social.setSessionKey(session.getSessionKey());
            if (StrUtil.isNotBlank(session.getUnionId())) {
                social.setUnionId(session.getUnionId());
            }
            memberSocialUserMapper.update(social);
        }

        assertTrue(user.getStatus() != null && user.getStatus() == STATUS_ENABLED,
                MemberErrorCode.MEMBER_DISABLED);

        MemberLoginVO respVO = doLogin(user, resolveDevice(param.getDevice()), clientIp);
        respVO.setNewRegister(newRegister);
        return respVO;
    }

    /**
     * 账号密码登录
     */
    public MemberLoginVO passwordLogin(MemberLoginParam param, String clientIp) {
        // 验证码前置校验（一次性，防自动化爆破；验证码开关关闭时跳过）
        validateCaptcha(param.getCaptchaId(), param.getCaptchaData());

        MemberUser user = memberUserMapper.selectByMobile(param.getMobile());
        assertNotNull(user, MemberErrorCode.LOGIN_PASSWORD_INCORRECT);
        assertTrue(StrUtil.isNotBlank(user.getPassword())
                        && BCrypt.checkpw(param.getPassword(), user.getPassword()),
                MemberErrorCode.LOGIN_PASSWORD_INCORRECT);
        assertTrue(user.getStatus() != null && user.getStatus() == STATUS_ENABLED,
                MemberErrorCode.MEMBER_DISABLED);

        MemberLoginVO respVO = doLogin(user, resolveDevice(param.getDevice()), clientIp);
        respVO.setNewRegister(false);
        return respVO;
    }

    /**
     * 注销当前登录
     */
    public void logout() {
        StpMemberUtil.logout();
    }

    /**
     * 当前登录会员信息
     */
    public MemberInfoVO getUserInfo() {
        Long userId = StpMemberUtil.getLoginIdAsLong();
        MemberUser user = memberUserMapper.selectOneById(userId);
        assertNotNull(user, MemberErrorCode.MEMBER_NOT_FOUND, userId);

        MemberInfoVO vo = new MemberInfoVO();
        vo.setUserId(user.getId());
        vo.setMobile(user.getMobile());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());
        return vo;
    }

    // ================ 内部方法 ================

    private void validateCaptcha(String captchaId, String captchaData) {
        if (captchaService == null || !captchaService.isLoginRequired()) {
            return;
        }
        assertTrue(captchaService.validate(captchaId, captchaData),
                MemberErrorCode.CAPTCHA_INVALID);
    }

    private MemberUser registerByWxMini(WxMaSession session, String clientIp) {
        MemberUser user = new MemberUser();
        user.setMobile("");
        user.setPassword("");
        user.setNickname("微信用户");
        user.setAvatar("");
        user.setGender(0);
        user.setStatus(STATUS_ENABLED);
        user.setRegisterIp(clientIp);
        user.setRegisterType(MemberSocialTypeConstants.WECHAT_MINI);
        memberUserMapper.insert(user);
        return user;
    }

    private MemberSocialUser bindWxMiniSocial(Long userId, WxMaSession session) {
        MemberSocialUser social = new MemberSocialUser();
        social.setUserId(userId);
        social.setSocialType(MemberSocialTypeConstants.WECHAT_MINI);
        social.setOpenId(session.getOpenId());
        social.setUnionId(StrUtil.nullToEmpty(session.getUnionId()));
        social.setSessionKey(StrUtil.nullToEmpty(session.getSessionKey()));
        social.setNickname("");
        social.setAvatar("");
        social.setRawInfo("");
        memberSocialUserMapper.insert(social);
        return social;
    }

    private MemberLoginVO doLogin(MemberUser user, String device, String clientIp) {
        // 同端互踢
        StpMemberUtil.logout(user.getId(), device);
        StpMemberUtil.login(user.getId(), device);

        MemberUser update = new MemberUser();
        update.setId(user.getId());
        update.setLoginIp(clientIp);
        update.setLoginDate(LocalDateTime.now());
        memberUserMapper.update(update);

        MemberLoginVO respVO = new MemberLoginVO();
        respVO.setUserId(user.getId());
        respVO.setTokenName(StpMemberUtil.getTokenInfo().getTokenName());
        respVO.setTokenValue(StpMemberUtil.getTokenValue());
        respVO.setNickname(user.getNickname());
        return respVO;
    }

    private String resolveDevice(String device) {
        if (device == null || device.isBlank()) {
            return DeviceTypeConstants.MINI;
        }
        return device.trim().toUpperCase();
    }

}
