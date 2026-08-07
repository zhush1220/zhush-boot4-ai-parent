package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionWithdrawApplyQuery
 * @Description 提现申请表 分页查询请求
 * @Author zhush
 * @Date 2026/08/07
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "提现申请表分页查询请求")
public class UnionWithdrawApplyQuery extends BaseQuery {

    @Schema(description = "申请单号（业务唯一，格式：WD + 日期 + 序列号）")
    private String applyNo;

    @Schema(description = "推广者ID")
    private Long memberId;

    @Schema(description = "推广者姓名（冗余，便于展示）")
    private String memberName;

    @Schema(description = "推广者手机号（冗余，便于联系）")
    private String memberMobile;

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

    @Schema(description = "审核备注（驳回原因填这里）")
    private String auditRemark;

    @Schema(description = "打款操作人ID")
    private Long payOperatorId;

    @Schema(description = "打款操作人姓名")
    private String payOperatorName;

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

    @Schema(description = "取消原因")
    private String cancelReason;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "扩展JSON（如：{\"voucher_url\": \"xxx\"}）")
    private String extraJson;

}
