package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionCommissionRecordQuery
 * @Description 佣金记录表 分页查询请求
 * @Author zhush
 * @Date 2026/08/07
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "佣金记录表分页查询请求")
public class UnionCommissionRecordQuery extends BaseQuery {

    @Schema(description = "关联union_order.id")
    private Long orderId;

    @Schema(description = "推广者ID")
    private Long memberId;

    @Schema(description = "实际推广者ID（可能是外部推广者）")
    private Long actualPromoterId;

    @Schema(description = "实际推广者类型：1-系统会员 2-外部推广者")
    private Integer actualPromoterType;

    @Schema(description = "外部推广者ID")
    private Long externalPromoterId;

    @Schema(description = "结算目标类型：1-系统会员 2-团长代收")
    private Integer settleTargetType;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "状态：1-待计算 2-待入账 3-已入账 4-入账失败 5-已退款 6-已取消")
    private Integer status;

    @Schema(description = "确认状态：0-待确认 1-已确认 2-待修正 3-已驳回")
    private Integer confirmStatus;

    @Schema(description = "结算批次号")
    private String settleBatchNo;

    @Schema(description = "关联资金流水ID")
    private String transactionId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "重试次数")
    private Integer retryCount;

}
