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

    @Schema(description = "token 值")
    private String tokenValue;

    @Schema(description = "昵称")
    private String nickname;

}
