package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionShortUrlMapQuery
 * @Description 短链接映射表 分页查询请求
 * @Author zhush
 * @Date 2026/08/08
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "短链接映射表分页查询请求")
public class UnionShortUrlMapQuery extends BaseQuery {

    @Schema(description = "短链码（如：abc123）")
    private String shortCode;

    @Schema(description = "关联推广链接ID")
    private Long promotionLinkId;

    @Schema(description = "原始推广链接")
    private String originalUrl;

    @Schema(description = "最终跳转URL（可做A/B测试或渠道跟踪）")
    private String targetUrl;

    @Schema(description = "总点击量")
    private Integer totalClicks;

    @Schema(description = "独立访客点击量（去重）")
    private Integer uniqueClicks;

    @Schema(description = "状态：1-正常 2-已禁用 3-已过期")
    private Integer status;

}
