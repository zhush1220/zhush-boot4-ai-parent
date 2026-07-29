package com.zhush.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @ClassName OpenUserVO
 * @Description 开放 API 用户信息返回（对第三方裁剪，不含手机号等敏感字段）
 * @Author zhush
 **/
@Data
@Schema(description = "开放 API 用户信息返回")
public class OpenUserVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像地址")
    private String avatar;

}
