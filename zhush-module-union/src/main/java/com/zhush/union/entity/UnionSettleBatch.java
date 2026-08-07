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
 * @ClassName UnionSettleBatch
 * @Description 结算批次表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_settle_batch")
public class UnionSettleBatch implements Serializable {

    @Serial
    private static final long serialVersionUID = -6913746645039494659L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 批次号（业务唯一）
     */
    private String batchNo;

    /**
     * 批次总金额
     */
    private BigDecimal totalAmount;

    /**
     * 包含记录数
     */
    private Integer recordCount;

    /**
     * 状态：1-待审核 2-审核通过 3-已结算 4-已驳回
     */
    private Integer status;

    /**
     * 操作员ID
     */
    private Long operatorId;

    /**
     * 审核时间
     */
    private LocalDateTime auditTime;

    /**
     * 结算完成时间
     */
    private LocalDateTime settleTime;

    /**
     * remark
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
