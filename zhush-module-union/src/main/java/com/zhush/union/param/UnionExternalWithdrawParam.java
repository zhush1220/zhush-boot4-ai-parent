package com.zhush.union.param;

import com.zhush.union.entity.UnionExternalWithdraw;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionExternalWithdrawParam
 * @Description 外部推广者提现申请表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionExternalWithdraw.class)
@Schema(description = "外部推广者提现申请表新增/修改请求参数")
public class UnionExternalWithdrawParam {

    @Schema(description = "ID（新增时不需要传）")
    private Long id;

    @Schema(description = "external_promoter_id")
    @NotNull(message = "external_promoter_id不能为空")
    private Long externalPromoterId;

    @Schema(description = "master_member_id")
    @NotNull(message = "master_member_id不能为空")
    private Long masterMemberId;

    @Schema(description = "申请单号")
    @NotBlank(message = "申请单号不能为空")
    private String applyNo;

    @Schema(description = "apply_amount")
    @NotNull(message = "apply_amount不能为空")
    private BigDecimal applyAmount;

    @Schema(description = "actual_amount")
    private BigDecimal actualAmount;

    @Schema(description = "fee_amount")
    private BigDecimal feeAmount;

    @Schema(description = "收款账号")
    @NotBlank(message = "收款账号不能为空")
    private String payAccount;

    @Schema(description = "收款人姓名")
    @NotBlank(message = "收款人姓名不能为空")
    private String payAccountName;

    @Schema(description = "1-待处理 2-已打款 3-已驳回")
    private Integer status;

    @Schema(description = "pay_time")
    private LocalDateTime payTime;

    @Schema(description = "transaction_id")
    private Long transactionId;

}
