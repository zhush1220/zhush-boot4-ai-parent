package com.zhush.system.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName Oauth2ClientQuery
 * @Description OAuth2 客户端分页查询请求
 * @Author zhush
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "OAuth2 客户端分页查询请求")
public class Oauth2ClientQuery extends BaseQuery {

    @Schema(description = "客户端标识（模糊匹配）")
    private String clientId;

    @Schema(description = "应用名称（模糊匹配）")
    private String clientName;

    @Schema(description = "状态：0-停用 1-启用")
    private Integer status;

}
