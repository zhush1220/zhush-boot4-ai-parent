package com.zhush.union.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @ClassName UnionProduct
 * @Description 联盟商品主表 实体类
 * @Author zhush
 * @Date 2026/8/7 0007 22:00
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_product")
public class UnionProduct extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = -201534406171737567L;

    /**
     * 平台：1-淘宝 2-京东 3-拼多多 4-抖音
     */
    private Integer platform;

    /**
     * 第三方平台原始商品ID（京东为字符串）
     */
    private String outerProductId;

    /**
     * 商品标题
     */
    private String title;

    /**
     * 副标题/卖点
     */
    private String subTitle;

    /**
     * 主图URL
     */
    private String imageUrl;

    /**
     * 商品轮播图列表，JSON数组
     */
    private String imageList;

    /**
     * 市场价（划掉的价格）
     */
    private BigDecimal marketPrice;

    /**
     * 当前售价
     */
    private BigDecimal salePrice;

    /**
     * 佣金比率（平台原始值，淘宝为千分比需转换）
     */
    private BigDecimal commissionRate;

    /**
     * 预估佣金金额（预计算字段）
     */
    private BigDecimal commissionAmount;

    /**
     * 店铺名称
     */
    private String shopName;

    /**
     * 店铺ID
     */
    private String shopId;

    /**
     * 类目ID
     */
    private String categoryId;

    /**
     * 类目名称
     */
    private String categoryName;

    /**
     * 月销量
     */
    private Integer salesVolume;

    /**
     * 好评数
     */
    private Integer goodComments;

    /**
     * 优惠券ID
     */
    private String couponId;

    /**
     * 优惠券面额
     */
    private BigDecimal couponAmount;

    /**
     * 优惠券开始时间
     */
    private LocalDateTime couponStartTime;

    /**
     * 优惠券结束时间
     */
    private LocalDateTime couponEndTime;

    /**
     * 是否推荐：0-否 1-是
     */
    private Boolean isRecommend;

    /**
     * 状态：1-上架 2-下架 3-审核中
     */
    private Integer status;

    /**
     * 同步状态：0-未同步 1-已同步 2-同步失败
     */
    private Integer syncStatus;

    /**
     * 最后同步时间
     */
    private LocalDateTime lastSyncTime;

    /**
     * 下次同步时间（用于增量同步）
     */
    private LocalDateTime nextSyncTime;


}
