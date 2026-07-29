package com.zhush.system.vo;

import com.zhush.system.entity.SysOpenApiLog;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @ClassName OpenApiLogVO
 * @Description 开放 API 调用日志详情 / 列表返回
 * @Author zhush
 **/
@Data
@AutoMapper(target = SysOpenApiLog.class)
@Schema(description = "开放 API 调用日志详情 / 列表返回")
public class OpenApiLogVO {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "客户端标识")
    private String clientId;

    @Schema(description = "授权用户ID（应用级调用为空）")
    private Long loginId;

    @Schema(description = "请求方式")
    private String requestMethod;

    @Schema(description = "请求地址")
    private String requestUri;

    @Schema(description = "HTTP 响应状态码")
    private Integer status;

    @Schema(description = "OAuth2 协议错误码（成功为空）")
    private String errorCode;

    @Schema(description = "执行耗时（毫秒）")
    private Long costMillis;

    @Schema(description = "调用方IP")
    private String clientIp;

    @Schema(description = "调用时间")
    private LocalDateTime callTime;

}
