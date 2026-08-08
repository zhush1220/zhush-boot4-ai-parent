package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionClickTrackingQuery
 * @Description 推广链接点击明细表 分页查询请求
 * @Author zhush
 * @Date 2026/08/08
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "推广链接点击明细表分页查询请求")
public class UnionClickTrackingQuery extends BaseQuery {

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

    @Schema(description = "关联订单ID（归因后填充）")
    private Long orderId;

}
