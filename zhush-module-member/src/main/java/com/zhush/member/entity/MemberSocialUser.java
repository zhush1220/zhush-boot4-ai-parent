package com.zhush.member.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 会员第三方授权（微信小程序、开放平台、支付宝等）
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("member_social_user")
public class MemberSocialUser extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 3038890361743345228L;

    /** 会员用户ID */
    private Long userId;

    /** 第三方类型：WECHAT_MINI / WECHAT_MP / WECHAT_OPEN / ALIPAY / APPLE ... */
    private String socialType;

    /** 第三方用户唯一标识（openid） */
    private String openId;

    /** 第三方 UnionId */
    private String unionId;

    /** 微信 session_key（小程序） */
    private String sessionKey;

    /** 第三方昵称 */
    private String nickname;

    /** 第三方头像 */
    private String avatar;

    /** 第三方原始信息（JSON） */
    private String rawInfo;
}
