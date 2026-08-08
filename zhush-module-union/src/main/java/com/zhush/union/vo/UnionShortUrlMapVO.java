package com.zhush.union.vo;

import com.zhush.union.entity.UnionShortUrlMap;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionShortUrlMapVO
 * @Description 短链接映射表 详情/列表返回
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionShortUrlMap.class)
@Schema(description = "短链接映射表详情/列表返回")
public class UnionShortUrlMapVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "短链码（如：abc123）")
    private String shortCode;

    @Schema(description = "关联推广链接ID")
    private Long promotionLinkId;

    @Schema(description = "原始推广链接")
    private String originalUrl;

    @Schema(description = "最终跳转URL（可做A/B测试或渠道跟踪）")
    private String targetUrl;

    @Schema(description = "过期时间（NULL表示永久）")
    private LocalDateTime expireTime;

    @Schema(description = "总点击量")
    private Integer totalClicks;

    @Schema(description = "独立访客点击量（去重）")
    private Integer uniqueClicks;

    @Schema(description = "最后点击时间")
    private LocalDateTime lastClickTime;

    @Schema(description = "状态：1-正常 2-已禁用 3-已过期")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
