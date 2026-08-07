package com.zhush.union.param;

import com.zhush.union.entity.UnionExternalCommissionPending;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionExternalCommissionPendingParam
 * @Description 外部推广者待结算佣金表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@AutoMapper(target = UnionExternalCommissionPending.class)
@Schema(description = "外部推广者待结算佣金表新增/修改请求参数")
public class UnionExternalCommissionPendingParam {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "外部推广者ID")
    private Long externalPromoterId;

    @Schema(description = "团长ID")
    private Long masterMemberId;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "待分配佣金")
    private BigDecimal commissionAmount;

    @Schema(description = "结算状态：0-待结算 1-已结算 2-已取消")
    private Integer settleStatus;

    @Schema(description = "结算时间")
    private LocalDateTime settleTime;

    @Schema(description = "备注")
    private String remark;

}
