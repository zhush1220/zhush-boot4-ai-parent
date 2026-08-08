package com.zhush.union.param;

import com.zhush.union.entity.UnionOrder;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionOrderParam
 * @Description 联盟订单主表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionOrder.class)
@Schema(description = "联盟订单主表新增/修改请求参数")
public class UnionOrderParam {

    @Schema(description = "ID（新增时不需要传）")
    private Long id;

    @Schema(description = "分表键（格式：YYYYMM）")
    private String shardingKey;

    @Schema(description = "平台：1-淘宝 2-京东 3-拼多多")
    @NotNull(message = "平台：1-淘宝 2-京东 3-拼多多不能为空")
    private Integer platform;

    @Schema(description = "订单来源：1-平台推送 2-定时轮询 3-手动录入")
    private Integer orderSource;

    @Schema(description = "第三方平台订单号")
    @NotBlank(message = "第三方平台订单号不能为空")
    private String outerOrderId;

    @Schema(description = "父订单号（合并支付时）")
    private String parentOrderId;

    @Schema(description = "推广者ID（归因后填充）")
    private Long memberId;

    @Schema(description = "推广者类型：1-系统会员 2-外部推广者")
    private Integer promoterType;

    @Schema(description = "外部推广者ID")
    private Long externalPromoterId;

    @Schema(description = "所属团长ID")
    private Long masterMemberId;

    @Schema(description = "关联推广链接记录ID")
    private Long promotionLinkId;

    @Schema(description = "关联union_product.id")
    private Long productId;

    @Schema(description = "商品标题（冗余）")
    private String productTitle;

    @Schema(description = "商品主图（冗余）")
    private String productImage;

    @Schema(description = "第三方商品ID（冗余）")
    private String outerProductId;

    @Schema(description = "店铺名称（冗余）")
    private String shopName;

    @Schema(description = "订单金额（实际支付）")
    @NotNull(message = "订单金额（实际支付）不能为空")
    private BigDecimal orderAmount;

    @Schema(description = "该笔订单的佣金比率（快照）")
    private BigDecimal commissionRate;

    @Schema(description = "预估佣金金额")
    private BigDecimal commissionAmount;

    @Schema(description = "最终结算佣金（平台结算后确定）")
    private BigDecimal actualCommission;

    @Schema(description = "优惠券抵扣金额")
    private BigDecimal couponAmount;

    @Schema(description = "订单状态：1-待支付 2-已支付 3-已收货 4-已结算 5-无效/退款 6-已关闭")
    private Integer orderStatus;

    @Schema(description = "平台原始订单状态码")
    private String platformOrderStatus;

    @Schema(description = "结算状态：0-未结算 1-已结算 2-结算失败")
    private Integer settleStatus;

    @Schema(description = "实际结算时间")
    private LocalDateTime settleTime;

    @Schema(description = "下单时间（第三方）")
    @NotNull(message = "下单时间（第三方）不能为空")
    private LocalDateTime orderTime;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "确认收货时间")
    private LocalDateTime confirmTime;

    @Schema(description = "同步到本系统的时间")
    private LocalDateTime syncTime;

    @Schema(description = "平台扩展数据（JSON）")
    private String extraJson;

}
