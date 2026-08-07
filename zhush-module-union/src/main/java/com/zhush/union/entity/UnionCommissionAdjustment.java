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
 * @ClassName UnionCommissionAdjustment
 * @Description 佣金调整记录表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_commission_adjustment")
public class UnionCommissionAdjustment implements Serializable {

    @Serial
    private static final long serialVersionUID = -4852308231170041526L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 推广者ID
     */
    private Long memberId;

    /**
     * 调整单号
     */
    private String adjustNo;

    /**
     * 调整类型：1-补偿 2-扣减 3-修正
     */
    private Integer adjustType;

    /**
     * 调整金额（正数增加，负数减少）
     */
    private BigDecimal adjustAmount;

    /**
     * 调整前余额
     */
    private BigDecimal beforeBalance;

    /**
     * 调整后余额
     */
    private BigDecimal afterBalance;

    /**
     * 关联订单ID（如订单佣金修正）
     */
    private Long orderId;

    /**
     * 调整原因
     */
    private String reason;

    /**
     * 附件（凭证图片）
     */
    private String attachments;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 1-生效 2-已撤销
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
