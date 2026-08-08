package com.zhush.union.param;

import com.zhush.union.entity.UnionExternalCommissionDetail;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionExternalCommissionDetailParam
 * @Description 外部推广者佣金明细表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionExternalCommissionDetail.class)
@Schema(description = "外部推广者佣金明细表新增/修改请求参数")
public class UnionExternalCommissionDetailParam {

    @Schema(description = "ID（新增时不需要传）")
    private Long id;

    @Schema(description = "外部推广者ID")
    @NotNull(message = "外部推广者ID不能为空")
    private Long externalPromoterId;

    @Schema(description = "所属团长ID")
    @NotNull(message = "所属团长ID不能为空")
    private Long masterMemberId;

    @Schema(description = "关联订单ID")
    @NotNull(message = "关联订单ID不能为空")
    private Long orderId;

    @Schema(description = "订单金额")
    @NotNull(message = "订单金额不能为空")
    private BigDecimal orderAmount;

    @Schema(description = "佣金比率")
    @NotNull(message = "佣金比率不能为空")
    private BigDecimal commissionRate;

    @Schema(description = "佣金金额")
    @NotNull(message = "佣金金额不能为空")
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
