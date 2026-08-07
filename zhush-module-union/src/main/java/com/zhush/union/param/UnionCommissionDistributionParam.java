package com.zhush.union.param;

import com.zhush.union.entity.UnionCommissionDistribution;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionCommissionDistributionParam
 * @Description 佣金分配明细表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@AutoMapper(target = UnionCommissionDistribution.class)
@Schema(description = "佣金分配明细表新增/修改请求参数")
public class UnionCommissionDistributionParam {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "佣金记录ID")
    private Long commissionRecordId;

    @Schema(description = "接收佣金的推广者ID")
    private Long receiverMemberId;

    @Schema(description = "接收人的层级（1=本人，2=上级，...）")
    private Integer receiverLevel;

    @Schema(description = "实际分配金额")
    private BigDecimal amount;

    @Schema(description = "分配比例（占可分配池）")
    private BigDecimal rate;

    @Schema(description = "类型：1-直接佣金 2-层级奖励 3-平台返利")
    private Integer distributionType;

    @Schema(description = "1-待入账 2-已入账 3-已失效")
    private Integer status;

}
