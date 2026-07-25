package com.zhush.system.enums;

import com.zhush.common.exception.IErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * system 模块错误码
 *
 * <p>模块号：{@code 001}
 * <ul>
 *     <li>{@code 1001xxx} —— 客户端错误</li>
 *     <li>{@code 2001xxx} —— 服务端错误</li>
 * </ul>
 *
 * @author zhush
 */
@Getter
@AllArgsConstructor
public enum SystemErrorCode implements IErrorCode {

    // ================ 用户 1001 0xx ================
    USER_NOT_FOUND(1001001, "用户不存在：{0}"),
    USER_DISABLED(1001002, "用户已被禁用"),
    USER_ALREADY_EXISTS(1001003, "用户已存在：{0}"),
    PASSWORD_INCORRECT(1001004, "账号或密码错误"),
    CAPTCHA_INVALID(1001005, "验证码错误或已过期"),
    USER_PASSWORD_REQUIRED(1001006, "新增用户时密码不能为空"),

    // ================ 角色 1001 1xx ================
    ROLE_NOT_FOUND(1001101, "角色不存在：{0}"),
    ROLE_HAS_USER(1001102, "角色下仍有用户，无法删除"),
    ROLE_CODE_DUPLICATE(1001103, "角色编码已存在：{0}"),

    // ================ 菜单 / 权限 1001 2xx ================
    MENU_NOT_FOUND(1001201, "菜单不存在：{0}"),
    MENU_HAS_CHILDREN(1001202, "存在子菜单，无法删除"),
    PERMISSION_DENIED(1001203, "无权限操作"),
    MENU_PARENT_ERROR(1001204, "父菜单不能为自身"),

    // ================ 部门 1001 3xx ================
    DEPT_NOT_FOUND(1001301, "部门不存在：{0}"),
    DEPT_HAS_CHILDREN(1001302, "存在子部门，无法删除"),
    DEPT_HAS_USER(1001303, "部门下仍有用户，无法删除"),

    // ================ 数据源 1001 4xx ================
    DATASOURCE_NOT_FOUND(1001401, "数据源不存在：{0}"),
    DATASOURCE_CONNECT_FAIL(1001402, "数据源连接失败：{0}"),

    // ================ 操作日志 1001 5xx ================
    OPERATE_LOG_NOT_FOUND(1001501, "操作日志不存在：{0}"),

    // ================ 系统级 2001 0xx ================
    SYSTEM_CONFIG_MISSING(2001001, "系统配置项缺失：{0}"),
    ;

    private final int code;
    private final String message;

}
