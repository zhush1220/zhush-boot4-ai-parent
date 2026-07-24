package com.zhush.member.vo;

import lombok.Data;

/**
 * 会员登录结果
 *
 * @author zhush
 */
@Data
public class MemberLoginVO {

    /** 会员用户ID */
    private Long userId;

    /** token 名称 */
    private String tokenName;

    /** token 值 */
    private String tokenValue;

    /** 昵称 */
    private String nickname;

    /** 是否新注册用户（静默注册） */
    private Boolean newRegister;
}
