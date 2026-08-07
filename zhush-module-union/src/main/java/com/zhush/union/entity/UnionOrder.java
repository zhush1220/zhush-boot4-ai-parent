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
 * @ClassName UnionOrder
 * @Description 联盟订单主表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_order")
public class UnionOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = -5761185953432809402L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 分表键（格式：YYYYMM）
     */
    private String shardingKey;

    /**
     * 平台：1-淘宝 2-京东 3-拼多多
     */
    private Integer platform;

    /**
     * 订单来源：1-平台推送 2-定时轮询 3-手动录入
     */
    private Integer orderSource;

    /**
     * 第三方平台订单号
     */
    private String outerOrderId;

    /**
     * 父订单号（合并支付时）
     */
    private String parentOrderId;

    /**
     * 推广者ID（归因后填充）
     */
    private Long memberId;

    /**
     * 推广者类型：1-系统会员 2-外部推广者
     */
    private Integer promoterType;

    /**
     * 外部推广者ID
     */
    private Long externalPromoterId;

    /**
     * 所属团长ID
     */
    private Long masterMemberId;

    /**
     * 关联推广链接记录ID
     */
    private Long promotionLinkId;

    /**
     * 关联union_product.id
     */
    private Long productId;

    /**
     * 商品标题（冗余）
     */
    private String productTitle;

    /**
     * 商品主图（冗余）
     */
    private String productImage;

    /**
     * 第三方商品ID（冗余）
     */
    private String outerProductId;

    /**
     * 店铺名称（冗余）
     */
    private String shopName;

    /**
     * 订单金额（实际支付）
     */
    private BigDecimal orderAmount;

    /**
     * 该笔订单的佣金比率（快照）
     */
    private BigDecimal commissionRate;

    /**
     * 预估佣金金额
     */
    private BigDecimal commissionAmount;

    /**
     * 最终结算佣金（平台结算后确定）
     */
    private BigDecimal actualCommission;

    /**
     * 优惠券抵扣金额
     */
    private BigDecimal couponAmount;

    /**
     * 订单状态：1-待支付 2-已支付 3-已收货 4-已结算 5-无效/退款 6-已关闭
     */
    private Integer orderStatus;

    /**
     * 平台原始订单状态码
     */
    private String platformOrderStatus;

    /**
     * 结算状态：0-未结算 1-已结算 2-结算失败
     */
    private Integer settleStatus;

    /**
     * 实际结算时间
     */
    private LocalDateTime settleTime;

    /**
     * 下单时间（第三方）
     */
    private LocalDateTime orderTime;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 确认收货时间
     */
    private LocalDateTime confirmTime;

    /**
     * 同步到本系统的时间
     */
    private LocalDateTime syncTime;

    /**
     * 平台扩展数据（JSON）
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
