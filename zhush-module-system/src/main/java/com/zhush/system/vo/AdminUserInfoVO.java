package com.zhush.system.vo;

import lombok.Data;

import java.util.List;

/**
 * @ClassName AdminUserInfoVO
 * @Description 管理端登录用户信息
 * @Author zhush
 * @Date 2026/7/20 0020 22:05
 **/
@Data
public class AdminUserInfoVO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 登录账号
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 角色编码列表
     */
    private List<String> roles;

    /**
     * 权限标识列表（超级管理员为 ["*"]）
     */
    private List<String> permissions;

}
