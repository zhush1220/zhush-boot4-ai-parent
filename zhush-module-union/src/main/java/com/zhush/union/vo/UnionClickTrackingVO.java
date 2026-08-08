package com.zhush.union.vo;

import com.zhush.union.entity.UnionClickTracking;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionClickTrackingVO
 * @Description 推广链接点击明细表 详情/列表返回
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionClickTracking.class)
@Schema(description = "推广链接点击明细表详情/列表返回")
public class UnionClickTrackingVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "短链码")
    private String shortCode;

    @Schema(description = "推广链接ID")
    private Long promotionLinkId;

    @Schema(description = "推广者ID")
    private Long memberId;

    @Schema(description = "平台")
    private Integer platform;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "登录用户ID（如果已登录）")
    private Long userId;

    @Schema(description = "访客ID（设备指纹/Cookie）")
    private String visitorId;

    @Schema(description = "点击IP")
    private String clickIp;

    @Schema(description = "用户代理")
    private String userAgent;

    @Schema(description = "来源页面")
    private String referer;

    @Schema(description = "省份")
    private String province;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "是否产生订单：0-否 1-是")
    private Boolean isConverted;

    @Schema(description = "关联订单ID（归因后填充）")
    private Long orderId;

    @Schema(description = "订单金额")
    private BigDecimal orderAmount;

    @Schema(description = "点击时间")
    private LocalDateTime clickTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
