package com.zhush.union.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @ClassName UnionClickTracking
 * @Description 推广链接点击明细表 实体类
 * @Author zhush
 * @Date 2026/8/7 0007 21:36
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table(value = "union_click_tracking")
public class UnionClickTracking extends BaseEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = -2012905187325718017L;


    /**
     * 短链码
     */
    @Column("short_code")
    private String shortCode;

    /**
     * 推广链接ID
     */
    @Column("promotion_link_id")
    private Long promotionLinkId;

    /**
     * 推广者ID
     */
    @Column("member_id")
    private Long memberId;

    /**
     * 平台：1-淘宝 2-京东 3-拼多多 4-抖音
     */
    private Integer platform;

    /**
     * 商品ID
     */
    @Column("product_id")
    private Long productId;

    /**
     * 登录用户ID（如果已登录）
     */
    @Column("user_id")
    private Long userId;

    /**
     * 访客ID（设备指纹/Cookie）
     */
    @Column("visitor_id")
    private String visitorId;

    /**
     * 点击IP
     */
    @Column("click_ip")
    private String clickIp;

    /**
     * 用户代理
     */
    @Column("user_agent")
    private String userAgent;

    /**
     * 来源页面
     */
    private String referer;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 是否产生订单：0-否 1-是
     */
    @Column("is_converted")
    private Boolean isConverted;

    /**
     * 关联订单ID（归因后填充）
     */
    @Column("order_id")
    private Long orderId;

    /**
     * 订单金额
     */
    @Column("order_amount")
    private BigDecimal orderAmount;

    /**
     * 点击时间
     */
    @Column("click_time")
    private LocalDateTime clickTime;

}
