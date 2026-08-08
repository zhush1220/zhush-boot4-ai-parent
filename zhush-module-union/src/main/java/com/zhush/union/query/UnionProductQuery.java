package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionProductQuery
 * @Description 联盟商品主表 分页查询请求
 * @Author zhush
 * @Date 2026/08/08
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "联盟商品主表分页查询请求")
public class UnionProductQuery extends BaseQuery {

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

    @Schema(description = "状态：1-上架 2-下架 3-审核中")
    private Integer status;

    @Schema(description = "同步状态：0-未同步 1-已同步 2-同步失败")
    private Integer syncStatus;

}
