package com.zhush.union.vo;

import com.zhush.union.entity.UnionExternalWithdraw;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionExternalWithdrawVO
 * @Description 外部推广者提现申请表 详情/列表返回
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@AutoMapper(target = UnionExternalWithdraw.class)
@Schema(description = "外部推广者提现申请表详情/列表返回")
public class UnionExternalWithdrawVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "external_promoter_id")
    private Long externalPromoterId;

    @Schema(description = "master_member_id")
    private Long masterMemberId;

    @Schema(description = "申请单号")
    private String applyNo;

    @Schema(description = "apply_amount")
    private BigDecimal applyAmount;

    @Schema(description = "actual_amount")
    private BigDecimal actualAmount;

    @Schema(description = "fee_amount")
    private BigDecimal feeAmount;

    @Schema(description = "收款账号")
    private String payAccount;

    @Schema(description = "收款人姓名")
    private String payAccountName;

    @Schema(description = "1-待处理 2-已打款 3-已驳回")
    private Integer status;

    @Schema(description = "pay_time")
    private LocalDateTime payTime;

    @Schema(description = "transaction_id")
    private Long transactionId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
