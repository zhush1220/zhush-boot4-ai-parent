package com.zhush.union.param;

import com.zhush.union.entity.UnionCommissionDistribution;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionCommissionDistributionParam
 * @Description 佣金分配明细表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionCommissionDistribution.class)
@Schema(description = "佣金分配明细表新增/修改请求参数")
public class UnionCommissionDistributionParam {

    @Schema(description = "ID（新增时不需要传）")
    private Long id;

    @Schema(description = "订单ID")
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "佣金记录ID")
    @NotNull(message = "佣金记录ID不能为空")
    private Long commissionRecordId;

    @Schema(description = "接收佣金的推广者ID")
    @NotNull(message = "接收佣金的推广者ID不能为空")
    private Long receiverMemberId;

    @Schema(description = "接收人的层级（1=本人，2=上级，...）")
    @NotNull(message = "接收人的层级（1=本人，2=上级，...）不能为空")
    private Integer receiverLevel;

    @Schema(description = "实际分配金额")
    @NotNull(message = "实际分配金额不能为空")
    private BigDecimal amount;

    @Schema(description = "分配比例（占可分配池）")
    @NotNull(message = "分配比例（占可分配池）不能为空")
    private BigDecimal rate;

    @Schema(description = "类型：1-直接佣金 2-层级奖励 3-平台返利")
    @NotNull(message = "类型：1-直接佣金 2-层级奖励 3-平台返利不能为空")
    private Integer distributionType;

    @Schema(description = "1-待入账 2-已入账 3-已失效")
    private Integer status;

}
