package com.zhush.union.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @ClassName UnionPromotionLink
 * @Description 推广链接表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_promotion_link")
public class UnionPromotionLink implements Serializable {

    @Serial
    private static final long serialVersionUID = -5925303119391442551L;

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
     * 推广者类型：1-系统会员 2-外部推广者
     */
    private Integer promoterType;

    /**
     * 外部推广者ID（关联union_external_promoter.id）
     */
    private Long externalPromoterId;

    /**
     * 所属团长ID（冗余，便于快速查询）
     */
    private Long masterMemberId;

    /**
     * 平台
     */
    private Integer platform;

    /**
     * 平台广告位ID（淘宝adzone_id）
     */
    private String platformAdzoneId;

    /**
     * 平台站点ID（淘宝site_id）
     */
    private String platformSiteId;

    /**
     * 商品ID（单品推广）
     */
    private Long productId;

    /**
     * 店铺ID（店铺推广）
     */
    private String shopId;

    /**
     * 链接类型：1-单品 2-店铺 3-活动 4-通用
     */
    private Integer linkType;

    /**
     * 推广长链接
     */
    private String promotionUrl;

    /**
     * 短链接
     */
    private String shortUrl;

    /**
     * 淘口令/口令码
     */
    private String promotionCode;

    /**
     * 有效期（NULL表示永久）
     */
    private LocalDateTime expireTime;

    /**
     * 点击量
     */
    private Integer clickCount;

    /**
     * 转化订单数
     */
    private Integer orderCount;

    /**
     * 1-正常 2-已失效
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
