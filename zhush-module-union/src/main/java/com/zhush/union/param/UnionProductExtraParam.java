package com.zhush.union.param;

import com.zhush.union.entity.UnionProductExtra;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionProductExtraParam
 * @Description 商品扩展信息表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@AutoMapper(target = UnionProductExtra.class)
@Schema(description = "商品扩展信息表新增/修改请求参数")
public class UnionProductExtraParam {

    @Schema(description = "ID")
    private Long id;

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

    @Schema(description = "运费")
    private BigDecimal shippingFee;

    @Schema(description = "售后服务")
    private String afterSaleService;

    @Schema(description = "其他扩展字段（JSON）")
    private String extraJson;

}
