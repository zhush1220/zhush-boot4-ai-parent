package com.zhush.system.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理员用户
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_admin_user")
public class SysAdminUser extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 登录账号 */
    private String username;

    /** 登录密码（BCrypt） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 真实姓名 */
    private String realName;

    /** 头像地址 */
    private String avatar;

    /** 性别：0-未知 1-男 2-女 */
    private Integer sex;

    /** 手机号 */
    private String mobile;

    /** 邮箱 */
    private String email;

    /** 部门ID */
    private Long deptId;

    /** 岗位ID */
    private Long postId;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    /** 最后登录IP */
    private String loginIp;

    /** 最后登录时间 */
    private LocalDateTime loginDate;

    /** 备注 */
    private String remark;
}
