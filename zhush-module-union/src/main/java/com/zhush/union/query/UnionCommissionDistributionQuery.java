package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionCommissionDistributionQuery
 * @Description 佣金分配明细表 分页查询请求
 * @Author zhush
 * @Date 2026/08/07
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "佣金分配明细表分页查询请求")
public class UnionCommissionDistributionQuery extends BaseQuery {

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "佣金记录ID")
    private Long commissionRecordId;

    @Schema(description = "接收佣金的推广者ID")
    private Long receiverMemberId;

    @Schema(description = "接收人的层级（1=本人，2=上级，...）")
    private Integer receiverLevel;

    @Schema(description = "类型：1-直接佣金 2-层级奖励 3-平台返利")
    private Integer distributionType;

    @Schema(description = "1-待入账 2-已入账 3-已失效")
    private Integer status;

}
