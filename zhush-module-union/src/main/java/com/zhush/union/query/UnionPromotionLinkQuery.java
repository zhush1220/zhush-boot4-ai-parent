package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionPromotionLinkQuery
 * @Description 推广链接表 分页查询请求
 * @Author zhush
 * @Date 2026/08/08
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "推广链接表分页查询请求")
public class UnionPromotionLinkQuery extends BaseQuery {

    @Schema(description = "推广者ID")
    private Long memberId;

    @Schema(description = "推广者类型：1-系统会员 2-外部推广者")
    private Integer promoterType;

    @Schema(description = "外部推广者ID（关联union_external_promoter.id）")
    private Long externalPromoterId;

    @Schema(description = "所属团长ID（冗余，便于快速查询）")
    private Long masterMemberId;

    @Schema(description = "平台")
    private Integer platform;

    @Schema(description = "平台广告位ID（淘宝adzone_id）")
    private String platformAdzoneId;

    @Schema(description = "平台站点ID（淘宝site_id）")
    private String platformSiteId;

    @Schema(description = "商品ID（单品推广）")
    private Long productId;

    @Schema(description = "店铺ID（店铺推广）")
    private String shopId;

    @Schema(description = "链接类型：1-单品 2-店铺 3-活动 4-通用")
    private Integer linkType;

    @Schema(description = "推广长链接")
    private String promotionUrl;

    @Schema(description = "短链接")
    private String shortUrl;

    @Schema(description = "淘口令/口令码")
    private String promotionCode;

    @Schema(description = "点击量")
    private Integer clickCount;

    @Schema(description = "转化订单数")
    private Integer orderCount;

    @Schema(description = "1-正常 2-已失效")
    private Integer status;

}
