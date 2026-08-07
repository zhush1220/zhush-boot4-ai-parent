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
 * @ClassName UnionCommissionRecord
 * @Description 佣金记录表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_commission_record")
public class UnionCommissionRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = -8501737572700445473L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 关联union_order.id
     */
    private Long orderId;

    /**
     * 推广者ID
     */
    private Long memberId;

    /**
     * 实际推广者ID（可能是外部推广者）
     */
    private Long actualPromoterId;

    /**
     * 实际推广者类型：1-系统会员 2-外部推广者
     */
    private Integer actualPromoterType;

    /**
     * 外部推广者ID
     */
    private Long externalPromoterId;

    /**
     * 结算目标类型：1-系统会员 2-团长代收
     */
    private Integer settleTargetType;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 订单金额
     */
    private BigDecimal orderAmount;

    /**
     * 佣金比率
     */
    private BigDecimal commissionRate;

    /**
     * 佣金金额（含税）
     */
    private BigDecimal commissionAmount;

    /**
     * 税金（如适用）
     */
    private BigDecimal taxAmount;

    /**
     * 实际入账金额（扣除税费后）
     */
    private BigDecimal actualAmount;

    /**
     * 状态：1-待计算 2-待入账 3-已入账 4-入账失败 5-已退款 6-已取消
     */
    private Integer status;

    /**
     * 确认状态：0-待确认 1-已确认 2-待修正 3-已驳回
     */
    private Integer confirmStatus;

    /**
     * 确认时间
     */
    private LocalDateTime confirmTime;

    /**
     * 结算批次号
     */
    private String settleBatchNo;

    /**
     * 入账时间
     */
    private LocalDateTime settleTime;

    /**
     * 关联资金流水ID
     */
    private String transactionId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
