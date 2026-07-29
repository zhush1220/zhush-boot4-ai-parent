package com.zhush.system.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * @ClassName OpenApiLogQuery
 * @Description 开放 API 调用日志分页查询请求
 * @Author zhush
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "开放 API 调用日志分页查询请求")
public class OpenApiLogQuery extends BaseQuery {

    @Schema(description = "客户端标识")
    private String clientId;

    @Schema(description = "授权用户ID")
    private Long loginId;

    @Schema(description = "请求地址（模糊匹配）")
    private String requestUri;

    @Schema(description = "HTTP 响应状态码")
    private Integer status;

    @Schema(description = "OAuth2 协议错误码（invalid_token / insufficient_scope...）")
    private String errorCode;

    @Schema(description = "调用时间-起")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime callTimeBegin;

    @Schema(description = "调用时间-止")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime callTimeEnd;

}
