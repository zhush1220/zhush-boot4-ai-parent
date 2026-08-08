package com.zhush.union.vo;

import com.zhush.union.entity.UnionExternalCommissionPending;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionExternalCommissionPendingVO
 * @Description 外部推广者待结算佣金表 详情/列表返回
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionExternalCommissionPending.class)
@Schema(description = "外部推广者待结算佣金表详情/列表返回")
public class UnionExternalCommissionPendingVO {

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

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
