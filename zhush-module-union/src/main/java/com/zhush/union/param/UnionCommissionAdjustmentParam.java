package com.zhush.union.param;

import com.zhush.union.entity.UnionCommissionAdjustment;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionCommissionAdjustmentParam
 * @Description 佣金调整记录表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionCommissionAdjustment.class)
@Schema(description = "佣金调整记录表新增/修改请求参数")
public class UnionCommissionAdjustmentParam {

    @Schema(description = "ID（新增时不需要传）")
    private Long id;

    @Schema(description = "推广者ID")
    @NotNull(message = "推广者ID不能为空")
    private Long memberId;

    @Schema(description = "调整单号")
    @NotBlank(message = "调整单号不能为空")
    private String adjustNo;

    @Schema(description = "调整类型：1-补偿 2-扣减 3-修正")
    @NotNull(message = "调整类型：1-补偿 2-扣减 3-修正不能为空")
    private Integer adjustType;

    @Schema(description = "调整金额（正数增加，负数减少）")
    @NotNull(message = "调整金额（正数增加，负数减少）不能为空")
    private BigDecimal adjustAmount;

    @Schema(description = "调整前余额")
    @NotNull(message = "调整前余额不能为空")
    private BigDecimal beforeBalance;

    @Schema(description = "调整后余额")
    @NotNull(message = "调整后余额不能为空")
    private BigDecimal afterBalance;

    @Schema(description = "关联订单ID（如订单佣金修正）")
    private Long orderId;

    @Schema(description = "调整原因")
    @NotBlank(message = "调整原因不能为空")
    private String reason;

    @Schema(description = "附件（凭证图片）")
    private String attachments;

    @Schema(description = "操作人ID")
    @NotNull(message = "操作人ID不能为空")
    private Long operatorId;

    @Schema(description = "操作人姓名")
    @NotBlank(message = "操作人姓名不能为空")
    private String operatorName;

    @Schema(description = "1-生效 2-已撤销")
    private Integer status;

}
