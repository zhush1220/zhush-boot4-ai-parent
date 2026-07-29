package com.zhush.system.vo;

import com.zhush.system.entity.SysOauth2Client;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @ClassName Oauth2ClientVO
 * @Description OAuth2 客户端详情 / 列表返回（不回显 clientSecret）
 * @Author zhush
 **/
@Data
@AutoMapper(target = SysOauth2Client.class)
@Schema(description = "OAuth2 客户端详情 / 列表返回")
public class Oauth2ClientVO {

    @Schema(description = "客户端ID")
    private Long id;

    @Schema(description = "客户端标识")
    private String clientId;

    @Schema(description = "应用名称")
    private String clientName;

    @Schema(description = "应用图标URL")
    private String logo;

    @Schema(description = "允许的授权类型，逗号分隔")
    private String grantTypes;

    @Schema(description = "允许申请的 scope，逗号分隔")
    private String scopes;

    @Schema(description = "回调地址白名单，逗号分隔")
    private String redirectUris;

    @Schema(description = "access_token 有效期（秒），空为全局默认")
    private Long accessTokenTimeout;

    @Schema(description = "refresh_token 有效期（秒），空为全局默认")
    private Long refreshTokenTimeout;

    @Schema(description = "开放 API 限流 QPS，空为全局默认")
    private Integer qpsLimit;

    @Schema(description = "开放 API 日调用配额，空为全局默认")
    private Integer dailyQuota;

    @Schema(description = "回调通知地址（预留）")
    private String notifyUrl;

    @Schema(description = "是否免授权确认页：0-否 1-是")
    private Integer autoConfirm;

    @Schema(description = "状态：0-停用 1-启用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
