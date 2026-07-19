package com.zhush.member.controller.app.vo;

import lombok.Data;

/**
 * 会员用户信息
 *
 * @author zhush
 */
@Data
public class MemberInfoRespVO {

    /** 会员用户ID */
    private Long userId;

    /** 手机号 */
    private String mobile;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 性别：0-未知 1-男 2-女 */
    private Integer gender;
}
