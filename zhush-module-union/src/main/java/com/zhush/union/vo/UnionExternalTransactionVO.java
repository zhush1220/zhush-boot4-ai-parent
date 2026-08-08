package com.zhush.union.vo;

import com.zhush.union.entity.UnionExternalTransaction;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionExternalTransactionVO
 * @Description 外部推广者资金流水表 详情/列表返回
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionExternalTransaction.class)
@Schema(description = "外部推广者资金流水表详情/列表返回")
public class UnionExternalTransactionVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "外部推广者ID")
    private Long externalPromoterId;

    @Schema(description = "所属团长ID")
    private Long masterMemberId;

    @Schema(description = "流水号（唯一）")
    private String transactionNo;

    @Schema(description = "类型：1-佣金入账 2-提现扣减 3-系统调整 4-退款回滚")
    private Integer transactionType;

    @Schema(description = "变动金额（正数增加，负数减少）")
    private BigDecimal amount;

    @Schema(description = "变动前余额")
    private BigDecimal balanceBefore;

    @Schema(description = "变动后余额")
    private BigDecimal balanceAfter;

    @Schema(description = "来源ID（如订单ID、提现申请ID）")
    private String sourceId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "1-成功 2-失败 3-处理中")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
