package com.zhush.union.vo;

import com.zhush.union.entity.UnionProduct;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionProductVO
 * @Description 联盟商品主表 详情/列表返回
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionProduct.class)
@Schema(description = "联盟商品主表详情/列表返回")
public class UnionProductVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "平台：1-淘宝 2-京东 3-拼多多 4-抖音")
    private Integer platform;

    @Schema(description = "第三方平台原始商品ID（京东为字符串）")
    private String outerProductId;

    @Schema(description = "商品标题")
    private String title;

    @Schema(description = "副标题/卖点")
    private String subTitle;

    @Schema(description = "主图URL")
    private String imageUrl;

    @Schema(description = "商品轮播图列表，JSON数组")
    private String imageList;

    @Schema(description = "市场价（划掉的价格）")
    private BigDecimal marketPrice;

    @Schema(description = "当前售价")
    private BigDecimal salePrice;

    @Schema(description = "佣金比率（平台原始值，淘宝为千分比需转换）")
    private BigDecimal commissionRate;

    @Schema(description = "预估佣金金额（预计算字段）")
    private BigDecimal commissionAmount;

    @Schema(description = "店铺名称")
    private String shopName;

    @Schema(description = "店铺ID")
    private String shopId;

    @Schema(description = "类目ID")
    private String categoryId;

    @Schema(description = "类目名称")
    private String categoryName;

    @Schema(description = "月销量")
    private Integer salesVolume;

    @Schema(description = "好评数")
    private Integer goodComments;

    @Schema(description = "优惠券ID")
    private String couponId;

    @Schema(description = "优惠券面额")
    private BigDecimal couponAmount;

    @Schema(description = "优惠券开始时间")
    private LocalDateTime couponStartTime;

    @Schema(description = "优惠券结束时间")
    private LocalDateTime couponEndTime;

    @Schema(description = "是否推荐：0-否 1-是")
    private Boolean isRecommend;

    @Schema(description = "状态：1-上架 2-下架 3-审核中")
    private Integer status;

    @Schema(description = "同步状态：0-未同步 1-已同步 2-同步失败")
    private Integer syncStatus;

    @Schema(description = "最后同步时间")
    private LocalDateTime lastSyncTime;

    @Schema(description = "下次同步时间（用于增量同步）")
    private LocalDateTime nextSyncTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
