package com.zhush.satoken.core;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpLogic;

/**
 * 会员端登录鉴权工具（账号类型 {@link LoginTypeConstants#MEMBER}）
 *
 * <p>会员端仅做登录态校验，不做权限（RBAC）校验；与管理端 {@link StpAdminUtil} 相互独立。
 *
 * @author zhush
 */
public final class StpMemberUtil {

    private StpMemberUtil() {
    }

    /**
     * 账号类型标识
     */
    public static final String TYPE = LoginTypeConstants.MEMBER;

    /**
     * 底层 StpLogic（构造时自动注册到 SaManager）
     */
    public static final StpLogic STP_LOGIC = new StpLogic(TYPE);

    // ================ 登录 ================

    public static void login(Object id) {
        STP_LOGIC.login(id);
    }

    /**
     * 指定设备类型登录（用于多端登录 / 同端互踢）
     */
    public static void login(Object id, String device) {
        STP_LOGIC.login(id, device);
    }

    // ================ 注销 ================

    public static void logout() {
        STP_LOGIC.logout();
    }

    public static void logout(Object loginId) {
        STP_LOGIC.logout(loginId);
    }

    /**
     * 注销指定账号在指定设备类型的会话（同端互踢）
     */
    public static void logout(Object loginId, String device) {
        STP_LOGIC.logout(loginId, device);
    }

    public static void kickout(Object loginId) {
        STP_LOGIC.kickout(loginId);
    }

    // ================ 会话状态 ================

    public static boolean isLogin() {
        return STP_LOGIC.isLogin();
    }

    public static void checkLogin() {
        STP_LOGIC.checkLogin();
    }

    public static Object getLoginId() {
        return STP_LOGIC.getLoginId();
    }

    public static long getLoginIdAsLong() {
        return STP_LOGIC.getLoginIdAsLong();
    }

    /**
     * 获取当前登录账号 id（未登录返回 {@code null}，不抛异常）
     */
    public static Long getLoginIdDefaultNull() {
        Object id = STP_LOGIC.getLoginIdDefaultNull();
        return id == null ? null : Long.valueOf(id.toString());
    }

    // ================ token ================

    public static String getTokenValue() {
        return STP_LOGIC.getTokenValue();
    }

    public static SaTokenInfo getTokenInfo() {
        return STP_LOGIC.getTokenInfo();
    }

}
