package com.zhush.member.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.zhush.member.controller.app.vo.MemberInfoRespVO;
import com.zhush.member.controller.app.vo.MemberLoginReqVO;
import com.zhush.member.controller.app.vo.MemberLoginRespVO;
import com.zhush.member.controller.app.vo.MemberWxMiniLoginReqVO;
import com.zhush.member.entity.MemberSocialUser;
import com.zhush.member.entity.MemberUser;
import com.zhush.member.enums.MemberErrorCode;
import com.zhush.member.mapper.MemberSocialUserMapper;
import com.zhush.member.mapper.MemberUserMapper;
import com.zhush.member.social.MemberSocialTypeConstants;
import com.zhush.member.social.WxMaSession;
import com.zhush.member.social.WxMiniClient;
import com.zhush.satoken.core.DeviceTypeConstants;
import com.zhush.satoken.core.StpMemberUtil;
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

    public MemberAuthService(MemberUserMapper memberUserMapper,
                             MemberSocialUserMapper memberSocialUserMapper,
                             WxMiniClient wxMiniClient) {
        this.memberUserMapper = memberUserMapper;
        this.memberSocialUserMapper = memberSocialUserMapper;
        this.wxMiniClient = wxMiniClient;
    }

    /**
     * 微信小程序登录：openid 不存在则静默注册
     */
    @Transactional(rollbackFor = Exception.class)
    public MemberLoginRespVO wxMiniLogin(MemberWxMiniLoginReqVO reqVO, String clientIp) {
        WxMaSession session = wxMiniClient.code2Session(reqVO.getCode());

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

        MemberLoginRespVO respVO = doLogin(user, resolveDevice(reqVO.getDevice()), clientIp);
        respVO.setNewRegister(newRegister);
        return respVO;
    }

    /**
     * 账号密码登录
     */
    public MemberLoginRespVO passwordLogin(MemberLoginReqVO reqVO, String clientIp) {
        MemberUser user = memberUserMapper.selectByMobile(reqVO.getMobile());
        assertNotNull(user, MemberErrorCode.LOGIN_PASSWORD_INCORRECT);
        assertTrue(StrUtil.isNotBlank(user.getPassword())
                        && BCrypt.checkpw(reqVO.getPassword(), user.getPassword()),
                MemberErrorCode.LOGIN_PASSWORD_INCORRECT);
        assertTrue(user.getStatus() != null && user.getStatus() == STATUS_ENABLED,
                MemberErrorCode.MEMBER_DISABLED);

        MemberLoginRespVO respVO = doLogin(user, resolveDevice(reqVO.getDevice()), clientIp);
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
    public MemberInfoRespVO getUserInfo() {
        Long userId = StpMemberUtil.getLoginIdAsLong();
        MemberUser user = memberUserMapper.selectOneById(userId);
        assertNotNull(user, MemberErrorCode.MEMBER_NOT_FOUND, userId);

        MemberInfoRespVO vo = new MemberInfoRespVO();
        vo.setUserId(user.getId());
        vo.setMobile(user.getMobile());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());
        return vo;
    }

    // ================ 内部方法 ================

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

    private MemberLoginRespVO doLogin(MemberUser user, String device, String clientIp) {
        // 同端互踢
        StpMemberUtil.logout(user.getId(), device);
        StpMemberUtil.login(user.getId(), device);

        MemberUser update = new MemberUser();
        update.setId(user.getId());
        update.setLoginIp(clientIp);
        update.setLoginDate(LocalDateTime.now());
        memberUserMapper.update(update);

        MemberLoginRespVO respVO = new MemberLoginRespVO();
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
