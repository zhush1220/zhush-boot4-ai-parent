package com.zhush.member.vo;

import com.zhush.member.entity.MemberUser;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员详情 / 列表返回
 *
 * @author zhush
 */
@Data
@AutoMapper(target = MemberUser.class)
public class MemberUserVO {

    /** 会员ID */
    private Long id;

    /** 手机号 */
    private String mobile;

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

    /** 创建时间 */
    private LocalDateTime createTime;
}
