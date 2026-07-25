package com.zhush.member.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 会员用户信息
 *
 * @author zhush
 */
@Data
@Schema(description = "会员用户信息")
public class MemberInfoVO {

    @Schema(description = "会员用户ID")
    private Long userId;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "性别：0-未知 1-男 2-女")
    private Integer gender;
}
