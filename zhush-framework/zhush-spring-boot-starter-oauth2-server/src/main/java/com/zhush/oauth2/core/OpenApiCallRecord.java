package com.zhush.oauth2.core;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 开放 API 调用记录（拦截器采集，业务侧异步落库）
 *
 * @author zhush
 */
@Data
public class OpenApiCallRecord {

    /**
     * 客户端标识（鉴权失败且无法解析时为 null）
     */
    private String clientId;

    /**
     * 授权用户 id（应用级调用为 null）
     */
    private Long loginId;

    /**
     * 请求方法（GET/POST...）
     */
    private String requestMethod;

    /**
     * 请求 URI
     */
    private String requestUri;

    /**
     * HTTP 响应状态码
     */
    private Integer status;

    /**
     * OAuth2 协议错误码（invalid_token / insufficient_scope...，成功为 null）
     */
    private String errorCode;

    /**
     * 耗时（毫秒）
     */
    private Long costMillis;

    /**
     * 调用方 IP
     */
    private String clientIp;

    /**
     * 调用时间
     */
    private LocalDateTime callTime;

}
