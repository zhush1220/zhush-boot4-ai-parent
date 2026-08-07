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
 * @ClassName UnionExternalWithdraw
 * @Description 外部推广者提现申请表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_external_withdraw")
public class UnionExternalWithdraw implements Serializable {

    @Serial
    private static final long serialVersionUID = -4034499221817647937L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * external_promoter_id
     */
    private Long externalPromoterId;

    /**
     * master_member_id
     */
    private Long masterMemberId;

    /**
     * 申请单号
     */
    private String applyNo;

    /**
     * apply_amount
     */
    private BigDecimal applyAmount;

    /**
     * actual_amount
     */
    private BigDecimal actualAmount;

    /**
     * fee_amount
     */
    private BigDecimal feeAmount;

    /**
     * 收款账号
     */
    private String payAccount;

    /**
     * 收款人姓名
     */
    private String payAccountName;

    /**
     * 1-待处理 2-已打款 3-已驳回
     */
    private Integer status;

    /**
     * pay_time
     */
    private LocalDateTime payTime;

    /**
     * transaction_id
     */
    private Long transactionId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
