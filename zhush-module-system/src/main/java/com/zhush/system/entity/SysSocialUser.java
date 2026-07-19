package com.zhush.system.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 管理端第三方登录绑定（预留：企业微信、钉钉等）
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_social_user")
public class SysSocialUser extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 管理员用户ID */
    private Long userId;

    /** 第三方类型：WECOM-企业微信 DINGTALK-钉钉 ... */
    private String socialType;

    /** 第三方用户唯一标识 */
    private String openId;

    /** 第三方 UnionId */
    private String unionId;

    /** 第三方昵称 */
    private String nickname;

    /** 第三方头像 */
    private String avatar;

    /** 第三方原始信息（JSON） */
    private String rawInfo;
}
