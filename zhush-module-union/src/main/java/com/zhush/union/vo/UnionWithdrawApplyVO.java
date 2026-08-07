package com.zhush.union.vo;

import com.zhush.union.entity.UnionWithdrawApply;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionWithdrawApplyVO
 * @Description 提现申请表 详情/列表返回
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@AutoMapper(target = UnionWithdrawApply.class)
@Schema(description = "提现申请表详情/列表返回")
public class UnionWithdrawApplyVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "申请单号（业务唯一，格式：WD + 日期 + 序列号）")
    private String applyNo;

    @Schema(description = "推广者ID")
    private Long memberId;

    @Schema(description = "推广者姓名（冗余，便于展示）")
    private String memberName;

    @Schema(description = "推广者手机号（冗余，便于联系）")
    private String memberMobile;

    @Schema(description = "申请提现金额")
    private BigDecimal applyAmount;

    @Schema(description = "实际到账金额（扣除税费/手续费后）")
    private BigDecimal actualAmount;

    @Schema(description = "手续费")
    private BigDecimal feeAmount;

    @Schema(description = "代扣税费")
    private BigDecimal taxAmount;

    @Schema(description = "税费率（如0.05表示5%）")
    private BigDecimal taxRate;

    @Schema(description = "提现方式：1-银行卡 2-支付宝 3-微信支付")
    private Integer withdrawType;

    @Schema(description = "收款账号（脱敏存储）")
    private String payAccount;

    @Schema(description = "收款人姓名")
    private String payAccountName;

    @Schema(description = "开户银行名称（银行卡时必填）")
    private String payBankName;

    @Schema(description = "开户支行名称")
    private String payBankBranch;

    @Schema(description = "状态：1-待审核 2-审核通过 3-已打款 4-已驳回 5-已取消 6-打款失败")
    private Integer status;

    @Schema(description = "审核人ID")
    private Long auditorId;

    @Schema(description = "审核人姓名（冗余）")
    private String auditorName;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "审核备注（驳回原因填这里）")
    private String auditRemark;

    @Schema(description = "打款操作人ID")
    private Long payOperatorId;

    @Schema(description = "打款操作人姓名")
    private String payOperatorName;

    @Schema(description = "打款时间")
    private LocalDateTime payTime;

    @Schema(description = "支付渠道订单号（如支付宝/微信流水号）")
    private String payChannelOrderNo;

    @Schema(description = "打款失败原因")
    private String payFailReason;

    @Schema(description = "打款重试次数")
    private Integer retryCount;

    @Schema(description = "关联资金流水ID（union_account_transaction.id）")
    private Long transactionId;

    @Schema(description = "关联资金流水号（冗余）")
    private String transactionNo;

    @Schema(description = "用户取消时间")
    private LocalDateTime cancelTime;

    @Schema(description = "取消原因")
    private String cancelReason;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "扩展JSON（如：{\"voucher_url\": \"xxx\"}）")
    private String extraJson;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
