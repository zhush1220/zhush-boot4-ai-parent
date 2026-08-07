package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionProductExtraQuery
 * @Description 商品扩展信息表 分页查询请求
 * @Author zhush
 * @Date 2026/08/07
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "商品扩展信息表分页查询请求")
public class UnionProductExtraQuery extends BaseQuery {

    @Schema(description = "关联union_product.id")
    private Long productId;

    @Schema(description = "商品详情页URL")
    private String detailUrl;

    @Schema(description = "商品描述（富文本）")
    private String desc;

    @Schema(description = "品牌名称")
    private String brandName;

    @Schema(description = "品牌ID")
    private String brandId;

    @Schema(description = "发货地省份")
    private String province;

    @Schema(description = "售后服务")
    private String afterSaleService;

    @Schema(description = "其他扩展字段（JSON）")
    private String extraJson;

}
