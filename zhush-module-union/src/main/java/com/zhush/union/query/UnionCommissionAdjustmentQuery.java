package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionCommissionAdjustmentQuery
 * @Description 佣金调整记录表 分页查询请求
 * @Author zhush
 * @Date 2026/08/07
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "佣金调整记录表分页查询请求")
public class UnionCommissionAdjustmentQuery extends BaseQuery {

    @Schema(description = "推广者ID")
    private Long memberId;

    @Schema(description = "调整单号")
    private String adjustNo;

    @Schema(description = "调整类型：1-补偿 2-扣减 3-修正")
    private Integer adjustType;

    @Schema(description = "关联订单ID（如订单佣金修正）")
    private Long orderId;

    @Schema(description = "调整原因")
    private String reason;

    @Schema(description = "附件（凭证图片）")
    private String attachments;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "1-生效 2-已撤销")
    private Integer status;

}
