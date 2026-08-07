package com.zhush.union.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @ClassName UnionWithdrawApply
 * @Description 提现申请表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_withdraw_apply")
public class UnionWithdrawApply implements Serializable {

    @Serial
    private static final long serialVersionUID = -2652226872508475159L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 申请单号（业务唯一，格式：WD + 日期 + 序列号）
     */
    private String applyNo;

    /**
     * 推广者ID
     */
    private Long memberId;

    /**
     * 推广者姓名（冗余，便于展示）
     */
    private String memberName;

    /**
     * 推广者手机号（冗余，便于联系）
     */
    private String memberMobile;

    /**
     * 申请提现金额
     */
    private BigDecimal applyAmount;

    /**
     * 实际到账金额（扣除税费/手续费后）
     */
    private BigDecimal actualAmount;

    /**
     * 手续费
     */
    private BigDecimal feeAmount;

    /**
     * 代扣税费
     */
    private BigDecimal taxAmount;

    /**
     * 税费率（如0.05表示5%）
     */
    private BigDecimal taxRate;

    /**
     * 提现方式：1-银行卡 2-支付宝 3-微信支付
     */
    private Integer withdrawType;

    /**
     * 收款账号（脱敏存储）
     */
    private String payAccount;

    /**
     * 收款人姓名
     */
    private String payAccountName;

    /**
     * 开户银行名称（银行卡时必填）
     */
    private String payBankName;

    /**
     * 开户支行名称
     */
    private String payBankBranch;

    /**
     * 状态：1-待审核 2-审核通过 3-已打款 4-已驳回 5-已取消 6-打款失败
     */
    private Integer status;

    /**
     * 审核人ID
     */
    private Long auditorId;

    /**
     * 审核人姓名（冗余）
     */
    private String auditorName;

    /**
     * 审核时间
     */
    private LocalDateTime auditTime;

    /**
     * 审核备注（驳回原因填这里）
     */
    private String auditRemark;

    /**
     * 打款操作人ID
     */
    private Long payOperatorId;

    /**
     * 打款操作人姓名
     */
    private String payOperatorName;

    /**
     * 打款时间
     */
    private LocalDateTime payTime;

    /**
     * 支付渠道订单号（如支付宝/微信流水号）
     */
    private String payChannelOrderNo;

    /**
     * 打款失败原因
     */
    private String payFailReason;

    /**
     * 打款重试次数
     */
    private Integer retryCount;

    /**
     * 关联资金流水ID（union_account_transaction.id）
     */
    private Long transactionId;

    /**
     * 关联资金流水号（冗余）
     */
    private String transactionNo;

    /**
     * 用户取消时间
     */
    private LocalDateTime cancelTime;

    /**
     * 取消原因
     */
    private String cancelReason;

    /**
     * 备注
     */
    private String remark;

    /**
     * 扩展JSON（如：{\"voucher_url\": \"xxx\"}）
     */
    private String extraJson;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
