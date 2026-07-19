package com.zhush.system.controller.admin.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理端第三方登录请求（预留：企业微信、钉钉等）
 *
 * @author zhush
 */
@Data
public class AdminSocialLoginReqVO {

    /** 第三方类型：WECOM-企业微信 DINGTALK-钉钉 ... */
    @NotBlank(message = "第三方类型不能为空")
    private String socialType;

    /** 第三方授权码 */
    @NotBlank(message = "授权码不能为空")
    private String code;

    /** 登录设备：PC / APP / MINI（不传默认 PC） */
    private String device;
}
