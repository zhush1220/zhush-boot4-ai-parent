package com.zhush.system.param;

import com.zhush.system.entity.SysAdminUser;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * @ClassName AdminUserParam
 * @Description 管理员新增/修改请求参数（新增时无需传 id，密码仅新增时生效）
 * @Author zhush
 * @Date 2026/7/20 0020 22:30
 **/
@Data
@AutoMapper(target = SysAdminUser.class)
public class AdminUserParam {

    /**
     * 用户ID（新增时不需要传）
     */
    private Long id;

    /**
     * 登录账号
     */
    @NotBlank(message = "登录账号不能为空")
    private String username;

    /**
     * 登录密码（明文，服务端 BCrypt 加密存储，仅新增时生效）
     */
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 头像地址
     */
    private String avatar;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 状态：0-禁用 1-启用（不传默认启用）
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 角色ID列表（非 null 时覆盖更新）
     */
    private List<Long> roleIds;

}
