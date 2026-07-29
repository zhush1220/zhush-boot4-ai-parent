package com.zhush.system.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 开放 API 调用日志
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_open_api_log")
public class SysOpenApiLog extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 客户端标识（鉴权失败且无法解析时为空） */
    private String clientId;

    /** 授权用户ID（应用级调用为空） */
    private Long loginId;

    /** 请求方式（GET/POST/PUT/DELETE） */
    private String requestMethod;

    /** 请求地址 */
    private String requestUri;

    /** HTTP 响应状态码 */
    private Integer status;

    /** OAuth2 协议错误码（invalid_token / insufficient_scope...，成功为空） */
    private String errorCode;

    /** 执行耗时（毫秒） */
    private Long costMillis;

    /** 调用方IP */
    private String clientIp;

    /** 调用时间 */
    private LocalDateTime callTime;
}
