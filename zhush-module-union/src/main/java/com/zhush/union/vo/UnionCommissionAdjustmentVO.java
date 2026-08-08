package com.zhush.union.vo;

import com.zhush.union.entity.UnionCommissionAdjustment;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionCommissionAdjustmentVO
 * @Description 佣金调整记录表 详情/列表返回
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionCommissionAdjustment.class)
@Schema(description = "佣金调整记录表详情/列表返回")
public class UnionCommissionAdjustmentVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "推广者ID")
    private Long memberId;

    @Schema(description = "调整单号")
    private String adjustNo;

    @Schema(description = "调整类型：1-补偿 2-扣减 3-修正")
    private Integer adjustType;

    @Schema(description = "调整金额（正数增加，负数减少）")
    private BigDecimal adjustAmount;

    @Schema(description = "调整前余额")
    private BigDecimal beforeBalance;

    @Schema(description = "调整后余额")
    private BigDecimal afterBalance;

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

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
