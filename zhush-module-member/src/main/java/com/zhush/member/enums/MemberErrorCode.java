package com.zhush.member.enums;

import com.zhush.common.exception.IErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * member 模块错误码
 *
 * <p>模块号：{@code 002}
 * <ul>
 *     <li>{@code 1002xxx} —— 客户端错误</li>
 *     <li>{@code 2002xxx} —— 服务端错误</li>
 * </ul>
 *
 * @author zhush
 */
@Getter
@AllArgsConstructor
public enum MemberErrorCode implements IErrorCode {

    // ================ 会员账号 1002 0xx ================
    MEMBER_NOT_FOUND(1002001, "会员不存在：{0}"),
    MEMBER_DISABLED(1002002, "会员已被禁用"),
    MEMBER_ALREADY_EXISTS(1002003, "会员已存在：{0}"),
    MOBILE_ALREADY_BOUND(1002004, "手机号已绑定其他账号"),
    EMAIL_ALREADY_BOUND(1002005, "邮箱已绑定其他账号"),
    CAPTCHA_INVALID(1002006, "验证码错误或已过期"),

    // ================ 登录 / 注册 1002 1xx ================
    LOGIN_PASSWORD_INCORRECT(1002101, "账号或密码错误"),
    LOGIN_LOCKED(1002102, "账号已锁定，请稍后再试"),
    REGISTER_CLOSED(1002103, "当前不允许注册"),
    WX_MINI_LOGIN_FAIL(1002104, "微信登录失败：{0}"),

    // ================ 积分 / 等级 1002 2xx ================
    POINT_NOT_ENOUGH(1002201, "积分不足"),
    LEVEL_NOT_FOUND(1002202, "会员等级不存在：{0}"),

    // ================ 地址 1002 3xx ================
    ADDRESS_NOT_FOUND(1002301, "收货地址不存在：{0}"),
    ADDRESS_LIMIT_EXCEED(1002302, "收货地址数量已达上限"),
    ;

    private final int code;
    private final String message;

}
