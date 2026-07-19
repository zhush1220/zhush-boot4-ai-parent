package com.zhush.member.social;

import lombok.Data;

/**
 * 微信小程序 code2session 结果
 *
 * @author zhush
 */
@Data
public class WxMaSession {

    /** 用户唯一标识 */
    private String openId;

    /** 同主体多应用唯一标识 */
    private String unionId;

    /** 会话密钥 */
    private String sessionKey;

}
