package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionExternalCommissionDetailQuery
 * @Description 外部推广者佣金明细表 分页查询请求
 * @Author zhush
 * @Date 2026/08/08
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "外部推广者佣金明细表分页查询请求")
public class UnionExternalCommissionDetailQuery extends BaseQuery {

    @Schema(description = "外部推广者ID")
    private Long externalPromoterId;

    @Schema(description = "所属团长ID")
    private Long masterMemberId;

    @Schema(description = "关联订单ID")
    private Long orderId;

    @Schema(description = "结算状态：0-待结算 1-已结算 2-已退款 3-结算失败")
    private Integer settleStatus;

    @Schema(description = "结算批次号")
    private String settleBatchNo;

    @Schema(description = "关联资金流水ID")
    private Long transactionId;

    @Schema(description = "备注")
    private String remark;

}
