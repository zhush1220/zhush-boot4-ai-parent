package com.zhush.satoken.core;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpLogic;

/**
 * 管理端登录鉴权工具（账号类型 {@link LoginTypeConstants#ADMIN}）
 *
 * <p>与会员端 {@link StpMemberUtil} 相互独立：使用各自的 token 名称与会话空间，
 * 管理员与会员可在同一浏览器/客户端同时保持登录态互不干扰。
 *
 * @author zhush
 */
public final class StpAdminUtil {

    private StpAdminUtil() {
    }

    /**
     * 账号类型标识
     */
    public static final String TYPE = LoginTypeConstants.ADMIN;

    /**
     * 底层 StpLogic（构造时自动注册到 SaManager，使全局 StpInterface 能按类型区分权限）
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

    /**
     * 踢下线（被踢者再次访问会提示已被踢出）
     */
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

    // ================ 权限校验 ================

    public static void checkPermission(String permission) {
        STP_LOGIC.checkPermission(permission);
    }

    public static boolean hasPermission(String permission) {
        return STP_LOGIC.hasPermission(permission);
    }

    public static void checkRole(String role) {
        STP_LOGIC.checkRole(role);
    }

    public static boolean hasRole(String role) {
        return STP_LOGIC.hasRole(role);
    }

}
