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
 * @ClassName UnionExternalCommissionPending
 * @Description 外部推广者待结算佣金表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_external_commission_pending")
public class UnionExternalCommissionPending implements Serializable {

    @Serial
    private static final long serialVersionUID = -7432532095292111530L;

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
     * 团长ID
     */
    private Long masterMemberId;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 待分配佣金
     */
    private BigDecimal commissionAmount;

    /**
     * 结算状态：0-待结算 1-已结算 2-已取消
     */
    private Integer settleStatus;

    /**
     * 结算时间
     */
    private LocalDateTime settleTime;

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
