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
 * @ClassName UnionCommissionDistribution
 * @Description 佣金分配明细表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_commission_distribution")
public class UnionCommissionDistribution implements Serializable {

    @Serial
    private static final long serialVersionUID = -2890994035404201719L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 佣金记录ID
     */
    private Long commissionRecordId;

    /**
     * 接收佣金的推广者ID
     */
    private Long receiverMemberId;

    /**
     * 接收人的层级（1=本人，2=上级，...）
     */
    private Integer receiverLevel;

    /**
     * 实际分配金额
     */
    private BigDecimal amount;

    /**
     * 分配比例（占可分配池）
     */
    private BigDecimal rate;

    /**
     * 类型：1-直接佣金 2-层级奖励 3-平台返利
     */
    private Integer distributionType;

    /**
     * 1-待入账 2-已入账 3-已失效
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
