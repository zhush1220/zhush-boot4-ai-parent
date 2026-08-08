package com.zhush.union.vo;

import com.zhush.union.entity.UnionCommissionRecord;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionCommissionRecordVO
 * @Description 佣金记录表 详情/列表返回
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionCommissionRecord.class)
@Schema(description = "佣金记录表详情/列表返回")
public class UnionCommissionRecordVO {

    @Schema(description = "ID")
    private Long id;

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

    @Schema(description = "订单金额")
    private BigDecimal orderAmount;

    @Schema(description = "佣金比率")
    private BigDecimal commissionRate;

    @Schema(description = "佣金金额（含税）")
    private BigDecimal commissionAmount;

    @Schema(description = "税金（如适用）")
    private BigDecimal taxAmount;

    @Schema(description = "实际入账金额（扣除税费后）")
    private BigDecimal actualAmount;

    @Schema(description = "状态：1-待计算 2-待入账 3-已入账 4-入账失败 5-已退款 6-已取消")
    private Integer status;

    @Schema(description = "确认状态：0-待确认 1-已确认 2-待修正 3-已驳回")
    private Integer confirmStatus;

    @Schema(description = "确认时间")
    private LocalDateTime confirmTime;

    @Schema(description = "结算批次号")
    private String settleBatchNo;

    @Schema(description = "入账时间")
    private LocalDateTime settleTime;

    @Schema(description = "关联资金流水ID")
    private String transactionId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "重试次数")
    private Integer retryCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
