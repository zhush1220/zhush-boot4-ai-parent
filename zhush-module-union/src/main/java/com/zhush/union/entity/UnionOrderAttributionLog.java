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
 * @ClassName UnionOrderAttributionLog
 * @Description 订单归因日志表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_order_attribution_log")
public class UnionOrderAttributionLog implements Serializable {

    @Serial
    private static final long serialVersionUID = -1323562723480630462L;

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
     * platform
     */
    private Integer platform;

    /**
     * outer_order_id
     */
    private String outerOrderId;

    /**
     * 归因到的推广者ID
     */
    private Long attributedMemberId;

    /**
     * 归因方式：cookie/链接/设备ID
     */
    private String attributionMethod;

    /**
     * 置信度分数
     */
    private BigDecimal confidenceScore;

    /**
     * 0-待处理 1-成功 2-失败
     */
    private Integer processStatus;

    /**
     * 失败原因
     */
    private String errorMsg;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
