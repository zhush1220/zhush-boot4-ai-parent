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
    DEPT_PARENT_ERROR(1001304, "父部门不能为自身或其子部门"),

    // ================ 数据源 1001 4xx ================
    DATASOURCE_NOT_FOUND(1001401, "数据源不存在：{0}"),
    DATASOURCE_CONNECT_FAIL(1001402, "数据源连接失败：{0}"),

    // ================ 操作日志 1001 5xx ================
    OPERATE_LOG_NOT_FOUND(1001501, "操作日志不存在：{0}"),

    // ================ OAuth2 客户端 / 开放 API 1001 6xx ================
    OAUTH2_CLIENT_NOT_FOUND(1001601, "OAuth2 客户端不存在：{0}"),
    OAUTH2_CLIENT_ID_DUPLICATE(1001602, "客户端标识已存在：{0}"),
    OPEN_API_LOG_NOT_FOUND(1001603, "开放 API 调用日志不存在：{0}"),

    // ================ 文件 1001 7xx ================
    FILE_NOT_FOUND(1001701, "文件不存在：{0}"),
    FILE_NO_PERMISSION(1001702, "无权操作该文件：{0}"),

    // ================ 数据字典 1001 8xx ================
    DICT_TYPE_NOT_FOUND(1001801, "字典类型不存在：{0}"),
    DICT_TYPE_DUPLICATE(1001802, "字典类型编码已存在：{0}"),
    DICT_TYPE_HAS_DATA(1001803, "字典类型下仍有数据，无法删除"),
    DICT_DATA_NOT_FOUND(1001804, "字典数据不存在：{0}"),
    DICT_DATA_DUPLICATE(1001805, "字典数据键值已存在：{0}"),

    // ================ 登录日志 1001 9xx ================
    LOGIN_LOG_NOT_FOUND(1001901, "登录日志不存在：{0}"),

    // ================ 通知公告 1002 0xx ================
    NOTICE_NOT_FOUND(1002001, "公告不存在：{0}"),

    // ================ 系统级 2001 0xx ================
    SYSTEM_CONFIG_MISSING(2001001, "系统配置项缺失：{0}"),
    ;

    private final int code;
    private final String message;

}
