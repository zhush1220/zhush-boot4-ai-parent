package com.zhush.system.controller.admin.vo.user;

import com.zhush.system.entity.SysAdminUser;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 管理员修改请求（账号不可修改，密码通过重置接口单独处理）
 *
 * @author zhush
 */
@Data
@AutoMapper(target = SysAdminUser.class)
public class AdminUserUpdateReqVO {

    /** 用户ID */
    @NotNull(message = "用户ID不能为空")
    private Long id;

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

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 角色ID列表（非 null 时覆盖更新） */
    private List<Long> roleIds;
}
