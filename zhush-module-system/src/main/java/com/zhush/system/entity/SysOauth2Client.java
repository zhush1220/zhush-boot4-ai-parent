package com.zhush.system.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * OAuth2 客户端（第三方应用注册信息）
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_oauth2_client")
public class SysOauth2Client extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 客户端标识 */
    private String clientId;

    /** 客户端密钥（BCrypt 存储，仅创建时明文回显一次） */
    private String clientSecret;

    /** 应用名称（授权确认页展示） */
    private String clientName;

    /** 应用图标URL（授权确认页展示） */
    private String logo;

    /** 允许的授权类型，逗号分隔（authorization_code,refresh_token,client_credentials） */
    private String grantTypes;

    /** 允许申请的 scope，逗号分隔（openid,userinfo,roles,...） */
    private String scopes;

    /** 回调地址白名单，逗号分隔，精确匹配 */
    private String redirectUris;

    /** access_token 有效期（秒），NULL 用全局默认 */
    private Long accessTokenTimeout;

    /** refresh_token 有效期（秒），NULL 用全局默认 */
    private Long refreshTokenTimeout;

    /** 开放 API 限流 QPS，NULL 用全局默认 */
    private Integer qpsLimit;

    /** 开放 API 日调用配额，NULL 用全局默认 */
    private Integer dailyQuota;

    /** 回调通知地址（预留，事件推送用） */
    private String notifyUrl;

    /** 是否免授权确认页：0-否 1-是（自有信任应用） */
    private Integer autoConfirm;

    /** 状态：0-停用 1-启用（停用后存量令牌立即失效） */
    private Integer status;

    /** 备注 */
    private String remark;
}
