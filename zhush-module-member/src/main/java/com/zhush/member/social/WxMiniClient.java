package com.zhush.member.social;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zhush.member.enums.MemberErrorCode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.zhush.common.exception.ServiceExceptionUtil.exception;

/**
 * 微信小程序客户端
 *
 * <p>封装 {@code code2Session}：用小程序登录 {@code code} 换取 openid / unionid / session_key。
 *
 * @author zhush
 */
@Component
public class WxMiniClient {

    private static final String CODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";

    private final WxMaProperties properties;

    public WxMiniClient(WxMaProperties properties) {
        this.properties = properties;
    }

    /**
     * 使用登录 code 换取会话信息
     */
    public WxMaSession code2Session(String code) {
        if (StrUtil.isBlank(properties.getAppId()) || StrUtil.isBlank(properties.getSecret())) {
            throw exception(MemberErrorCode.WX_MINI_LOGIN_FAIL, "小程序 AppID/Secret 未配置");
        }

        Map<String, Object> params = new HashMap<>(4);
        params.put("appid", properties.getAppId());
        params.put("secret", properties.getSecret());
        params.put("js_code", code);
        params.put("grant_type", "authorization_code");

        String body;
        try {
            body = HttpUtil.get(CODE2SESSION_URL, params);
        } catch (Exception e) {
            throw exception(MemberErrorCode.WX_MINI_LOGIN_FAIL, "调用微信接口异常");
        }

        JSONObject json = JSONUtil.parseObj(body);
        Integer errcode = json.getInt("errcode");
        if (errcode != null && errcode != 0) {
            throw exception(MemberErrorCode.WX_MINI_LOGIN_FAIL, json.getStr("errmsg", "errcode=" + errcode));
        }
        String openId = json.getStr("openid");
        if (StrUtil.isBlank(openId)) {
            throw exception(MemberErrorCode.WX_MINI_LOGIN_FAIL, "未获取到 openid");
        }

        WxMaSession session = new WxMaSession();
        session.setOpenId(openId);
        session.setUnionId(json.getStr("unionid"));
        session.setSessionKey(json.getStr("session_key"));
        return session;
    }

}
