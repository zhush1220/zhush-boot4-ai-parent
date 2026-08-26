package com.zhush.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @ClassName AdminLoginVO
 * @Description 管理端登录结果
 * @Author zhush
 * @Date 2026/7/20 0020 21:56
 **/
@Data
@Schema(description = "管理端登录结果")
public class AdminLoginVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "token 名称（放入请求头的 key）")
    private String tokenName;

    @Schema(description = "token 值（同 accessToken，兼容旧字段）")
    private String tokenValue;

    @Schema(description = "访问令牌（access-token，2 小时，放入 Authorization 请求头）")
    private String accessToken;

    @Schema(description = "刷新令牌（refresh-token，7 天，浏览器经 HttpOnly Cookie 下发，非浏览器端需自行保存）")
    private String refreshToken;

    @Schema(description = "昵称")
    private String nickname;

}
