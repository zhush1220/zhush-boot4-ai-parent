package com.zhush.member.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 会员登录结果
 *
 * @author zhush
 */
@Data
@Schema(description = "会员登录结果")
public class MemberLoginVO {

    @Schema(description = "会员用户ID")
    private Long userId;

    @Schema(description = "token 名称")
    private String tokenName;

    @Schema(description = "token 值")
    private String tokenValue;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "是否新注册用户（静默注册）")
    private Boolean newRegister;
}
