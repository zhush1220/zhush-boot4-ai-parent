package com.zhush.system.param;

import com.zhush.system.entity.SysOauth2Client;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @ClassName Oauth2ClientParam
 * @Description OAuth2 客户端新增/修改请求参数（client_secret 由服务端生成，不接受传入）
 * @Author zhush
 **/
@Data
@AutoMapper(target = SysOauth2Client.class)
@Schema(description = "OAuth2 客户端新增/修改请求参数")
public class Oauth2ClientParam {

    @Schema(description = "客户端ID（新增时不需要传）")
    private Long id;

    @Schema(description = "客户端标识（如 mall-app），创建后不可修改", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "客户端标识不能为空")
    private String clientId;

    @Schema(description = "应用名称（授权确认页展示）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "应用名称不能为空")
    private String clientName;

    @Schema(description = "应用图标URL")
    private String logo;

    @Schema(description = "允许的授权类型，逗号分隔（authorization_code,refresh_token,client_credentials）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "授权类型不能为空")
    private String grantTypes;

    @Schema(description = "允许申请的 scope，逗号分隔（openid,userinfo,roles）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "授权范围不能为空")
    private String scopes;

    @Schema(description = "回调地址白名单，逗号分隔，精确匹配（授权码模式必填）")
    private String redirectUris;

    @Schema(description = "access_token 有效期（秒），不传用全局默认")
    private Long accessTokenTimeout;

    @Schema(description = "refresh_token 有效期（秒），不传用全局默认")
    private Long refreshTokenTimeout;

    @Schema(description = "开放 API 限流 QPS，不传用全局默认")
    private Integer qpsLimit;

    @Schema(description = "开放 API 日调用配额，不传用全局默认")
    private Integer dailyQuota;

    @Schema(description = "回调通知地址（预留）")
    private String notifyUrl;

    @Schema(description = "是否免授权确认页：0-否 1-是（自有信任应用）")
    private Integer autoConfirm;

    @Schema(description = "状态：0-停用 1-启用（不传默认启用）")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

}
