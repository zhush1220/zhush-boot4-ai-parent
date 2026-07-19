package com.zhush.system.controller.admin.vo.user;

import com.zhush.system.entity.SysAdminUser;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 管理员新增请求
 *
 * @author zhush
 */
@Data
@AutoMapper(target = SysAdminUser.class)
public class AdminUserSaveReqVO {

    /** 登录账号 */
    @NotBlank(message = "登录账号不能为空")
    private String username;

    /** 登录密码（明文，服务端 BCrypt 加密存储） */
    @NotBlank(message = "登录密码不能为空")
    private String password;

    /** 昵称 */
    private String nickname;

    /** 真实姓名 */
    private String realName;

    /** 头像地址 */
    private String avatar;

    /** 手机号 */
    private String mobile;

    /** 邮箱 */
    private String email;

    /** 部门ID */
    private Long deptId;

    /** 状态：0-禁用 1-启用（不传默认启用） */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 角色ID列表 */
    private List<Long> roleIds;
}
