package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionOrderAttributionLogQuery
 * @Description 订单归因日志表 分页查询请求
 * @Author zhush
 * @Date 2026/08/08
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "订单归因日志表分页查询请求")
public class UnionOrderAttributionLogQuery extends BaseQuery {

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "platform")
    private Integer platform;

    @Schema(description = "outer_order_id")
    private String outerOrderId;

    @Schema(description = "归因到的推广者ID")
    private Long attributedMemberId;

    @Schema(description = "归因方式：cookie/链接/设备ID")
    private String attributionMethod;

    @Schema(description = "0-待处理 1-成功 2-失败")
    private Integer processStatus;

    @Schema(description = "失败原因")
    private String errorMsg;

}
