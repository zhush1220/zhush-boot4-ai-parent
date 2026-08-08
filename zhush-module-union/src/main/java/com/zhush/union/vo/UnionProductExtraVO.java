package com.zhush.union.vo;

import com.zhush.union.entity.UnionProductExtra;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionProductExtraVO
 * @Description 商品扩展信息表 详情/列表返回
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionProductExtra.class)
@Schema(description = "商品扩展信息表详情/列表返回")
public class UnionProductExtraVO {

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

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
