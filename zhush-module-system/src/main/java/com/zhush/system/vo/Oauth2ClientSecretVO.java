package com.zhush.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @ClassName Oauth2ClientSecretVO
 * @Description 客户端密钥回显（仅创建 / 重置密钥时返回一次明文，库中只存 BCrypt 哈希）
 * @Author zhush
 **/
@Data
@Schema(description = "客户端密钥回显（明文仅此一次，请妥善保存）")
public class Oauth2ClientSecretVO {

    @Schema(description = "客户端ID")
    private Long id;

    @Schema(description = "客户端标识")
    private String clientId;

    @Schema(description = "客户端密钥明文（仅本次返回，丢失只能重置）")
    private String clientSecret;

}
