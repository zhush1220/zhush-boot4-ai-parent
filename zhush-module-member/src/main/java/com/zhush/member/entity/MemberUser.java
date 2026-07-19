package com.zhush.member.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员用户
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("member_user")
public class MemberUser extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 手机号 */
    private String mobile;

    /** 登录密码（BCrypt，未设置为空） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 头像地址 */
    private String avatar;

    /** 性别：0-未知 1-男 2-女 */
    private Integer gender;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    /** 注册IP */
    private String registerIp;

    /** 注册来源：WECHAT_MINI-小程序 PASSWORD-账号密码 */
    private String registerType;

    /** 最后登录IP */
    private String loginIp;

    /** 最后登录时间 */
    private LocalDateTime loginDate;
}
