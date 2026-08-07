package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionApiCallLogQuery
 * @Description 平台API调用日志表 分页查询请求
 * @Author zhush
 * @Date 2026/08/07
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "平台API调用日志表分页查询请求")
public class UnionApiCallLogQuery extends BaseQuery {

    @Schema(description = "平台")
    private Integer platform;

    @Schema(description = "API名称（如：taobao.tbk.item.get）")
    private String apiName;

    @Schema(description = "请求参数（脱敏）")
    private String requestParams;

    @Schema(description = "响应码")
    private String responseCode;

    @Schema(description = "响应消息")
    private String responseMsg;

    @Schema(description = "是否成功")
    private Integer isSuccess;

    @Schema(description = "耗时（毫秒）")
    private Integer duration;

}
