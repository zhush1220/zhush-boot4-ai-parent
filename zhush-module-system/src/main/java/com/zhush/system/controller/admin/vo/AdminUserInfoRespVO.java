package com.zhush.system.controller.admin.vo;

import lombok.Data;

import java.util.List;

/**
 * 管理端登录用户信息
 *
 * @author zhush
 */
@Data
public class AdminUserInfoRespVO {

    /** 用户ID */
    private Long userId;

    /** 登录账号 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 角色编码列表 */
    private List<String> roles;

    /** 权限标识列表（超级管理员为 ["*"]） */
    private List<String> permissions;
}
