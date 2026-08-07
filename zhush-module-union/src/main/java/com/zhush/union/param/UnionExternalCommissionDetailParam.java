package com.zhush.union.param;

import com.zhush.union.entity.UnionExternalCommissionDetail;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionExternalCommissionDetailParam
 * @Description 外部推广者佣金明细表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@AutoMapper(target = UnionExternalCommissionDetail.class)
@Schema(description = "外部推广者佣金明细表新增/修改请求参数")
public class UnionExternalCommissionDetailParam {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "外部推广者ID")
    private Long externalPromoterId;

    @Schema(description = "所属团长ID")
    private Long masterMemberId;

    @Schema(description = "关联订单ID")
    private Long orderId;

    @Schema(description = "订单金额")
    private BigDecimal orderAmount;

    @Schema(description = "佣金比率")
    private BigDecimal commissionRate;

    @Schema(description = "佣金金额")
    private BigDecimal commissionAmount;

    @Schema(description = "结算状态：0-待结算 1-已结算 2-已退款 3-结算失败")
    private Integer settleStatus;

    @Schema(description = "结算时间")
    private LocalDateTime settleTime;

    @Schema(description = "结算批次号")
    private String settleBatchNo;

    @Schema(description = "关联资金流水ID")
    private Long transactionId;

    @Schema(description = "备注")
    private String remark;

}
