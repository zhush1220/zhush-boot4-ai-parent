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
 * @ClassName UnionExternalTransaction
 * @Description 外部推广者资金流水表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_external_transaction")
public class UnionExternalTransaction implements Serializable {

    @Serial
    private static final long serialVersionUID = -2332599452528469937L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 外部推广者ID
     */
    private Long externalPromoterId;

    /**
     * 所属团长ID
     */
    private Long masterMemberId;

    /**
     * 流水号（唯一）
     */
    private String transactionNo;

    /**
     * 类型：1-佣金入账 2-提现扣减 3-系统调整 4-退款回滚
     */
    private Integer transactionType;

    /**
     * 变动金额（正数增加，负数减少）
     */
    private BigDecimal amount;

    /**
     * 变动前余额
     */
    private BigDecimal balanceBefore;

    /**
     * 变动后余额
     */
    private BigDecimal balanceAfter;

    /**
     * 来源ID（如订单ID、提现申请ID）
     */
    private String sourceId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 1-成功 2-失败 3-处理中
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
