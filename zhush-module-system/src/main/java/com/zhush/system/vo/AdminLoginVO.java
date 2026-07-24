package com.zhush.system.vo;

import lombok.Data;

/**
 * @ClassName AdminLoginVO
 * @Description 管理端登录结果
 * @Author zhush
 * @Date 2026/7/20 0020 21:56
 **/
@Data
public class AdminLoginVO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * token 名称（放入请求头的 key）
     */
    private String tokenName;

    /**
     * token 值
     */
    private String tokenValue;

    /**
     * 昵称
     */
    private String nickname;

}
