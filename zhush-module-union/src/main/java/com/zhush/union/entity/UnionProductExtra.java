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
 * @ClassName UnionProductExtra
 * @Description 商品扩展信息表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_product_extra")
public class UnionProductExtra implements Serializable {

    @Serial
    private static final long serialVersionUID = -8829126250897302594L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 关联union_product.id
     */
    private Long productId;

    /**
     * 商品详情页URL
     */
    private String detailUrl;

    /**
     * 商品描述（富文本）
     */
    private String desc;

    /**
     * 品牌名称
     */
    private String brandName;

    /**
     * 品牌ID
     */
    private String brandId;

    /**
     * 发货地省份
     */
    private String province;

    /**
     * 运费
     */
    private BigDecimal shippingFee;

    /**
     * 售后服务
     */
    private String afterSaleService;

    /**
     * 其他扩展字段（JSON）
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
