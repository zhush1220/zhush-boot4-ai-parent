package com.zhush.satoken.core;

/**
 * 登录账号类型常量
 *
 * <p>基于 Sa-Token 多账号体系：管理端与会员端使用彼此独立的 {@code StpLogic}，
 * token、会话、超时配置互不影响。
 *
 * @author zhush
 */
public final class LoginTypeConstants {

    private LoginTypeConstants() {
    }

    /**
     * 管理端账号类型（对应 {@code /admin/**}）
     */
    public static final String ADMIN = "admin";

    /**
     * 会员端账号类型（对应 {@code /member/**}）
     */
    public static final String MEMBER = "member";

}
