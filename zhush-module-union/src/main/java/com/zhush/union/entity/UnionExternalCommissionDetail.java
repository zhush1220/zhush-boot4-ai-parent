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
 * @ClassName UnionExternalCommissionDetail
 * @Description 外部推广者佣金明细表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_external_commission_detail")
public class UnionExternalCommissionDetail implements Serializable {

    @Serial
    private static final long serialVersionUID = -2839494178491723248L;

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
     * 关联订单ID
     */
    private Long orderId;

    /**
     * 订单金额
     */
    private BigDecimal orderAmount;

    /**
     * 佣金比率
     */
    private BigDecimal commissionRate;

    /**
     * 佣金金额
     */
    private BigDecimal commissionAmount;

    /**
     * 结算状态：0-待结算 1-已结算 2-已退款 3-结算失败
     */
    private Integer settleStatus;

    /**
     * 结算时间
     */
    private LocalDateTime settleTime;

    /**
     * 结算批次号
     */
    private String settleBatchNo;

    /**
     * 关联资金流水ID
     */
    private Long transactionId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
