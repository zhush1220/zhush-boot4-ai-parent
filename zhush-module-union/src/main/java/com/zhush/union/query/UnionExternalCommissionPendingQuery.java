package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionExternalCommissionPendingQuery
 * @Description 外部推广者待结算佣金表 分页查询请求
 * @Author zhush
 * @Date 2026/08/08
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "外部推广者待结算佣金表分页查询请求")
public class UnionExternalCommissionPendingQuery extends BaseQuery {

    @Schema(description = "外部推广者ID")
    private Long externalPromoterId;

    @Schema(description = "团长ID")
    private Long masterMemberId;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "结算状态：0-待结算 1-已结算 2-已取消")
    private Integer settleStatus;

    @Schema(description = "备注")
    private String remark;

}
