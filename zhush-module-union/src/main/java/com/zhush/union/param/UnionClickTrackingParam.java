package com.zhush.union.param;

import com.zhush.union.entity.UnionClickTracking;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionClickTrackingParam
 * @Description 推广链接点击明细表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionClickTracking.class)
@Schema(description = "推广链接点击明细表新增/修改请求参数")
public class UnionClickTrackingParam {

    @Schema(description = "ID（新增时不需要传）")
    private Long id;

    @Schema(description = "短链码")
    @NotBlank(message = "短链码不能为空")
    private String shortCode;

    @Schema(description = "推广链接ID")
    @NotNull(message = "推广链接ID不能为空")
    private Long promotionLinkId;

    @Schema(description = "推广者ID")
    @NotNull(message = "推广者ID不能为空")
    private Long memberId;

    @Schema(description = "平台")
    @NotNull(message = "平台不能为空")
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
    @NotNull(message = "点击时间不能为空")
    private LocalDateTime clickTime;

}
