package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionLinkConfigQuery
 * @Description 转链配置表 分页查询请求
 * @Author zhush
 * @Date 2026/08/08
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "转链配置表分页查询请求")
public class UnionLinkConfigQuery extends BaseQuery {

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "推广者等级（NULL表示全部）")
    private Integer memberLevel;

    @Schema(description = "平台（NULL表示全部）")
    private Integer platform;

    @Schema(description = "类目ID（NULL表示全部）")
    private Long categoryId;

    @Schema(description = "短链接有效期（天数）")
    private Integer shortUrlExpireDays;

    @Schema(description = "expire_strategy")
    private String expireStrategy;

    @Schema(description = "每分钟限流次数（0表示不限）")
    private Integer rateLimitPerMinute;

    @Schema(description = "每小时限流次数（0表示不限）")
    private Integer rateLimitPerHour;

    @Schema(description = "优先级")
    private Integer priority;

    @Schema(description = "1-启用 0-禁用")
    private Integer status;

}
