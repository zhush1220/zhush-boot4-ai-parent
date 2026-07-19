package com.zhush.system.controller.admin.vo;

import lombok.Data;

/**
 * 管理端登录结果
 *
 * @author zhush
 */
@Data
public class AdminLoginRespVO {

    /** 用户ID */
    private Long userId;

    /** token 名称（放入请求头的 key） */
    private String tokenName;

    /** token 值 */
    private String tokenValue;

    /** 昵称 */
    private String nickname;
}
