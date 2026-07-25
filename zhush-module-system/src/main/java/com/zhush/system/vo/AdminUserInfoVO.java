package com.zhush.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @ClassName AdminUserInfoVO
 * @Description 管理端登录用户信息
 * @Author zhush
 * @Date 2026/7/20 0020 22:05
 **/
@Data
@Schema(description = "管理端登录用户信息")
public class AdminUserInfoVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "角色编码列表")
    private List<String> roles;

    @Schema(description = "权限标识列表（超级管理员为 [\"*\"]）")
    private List<String> permissions;

}
