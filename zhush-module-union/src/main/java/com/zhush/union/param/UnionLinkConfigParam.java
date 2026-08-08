package com.zhush.union.param;

import com.zhush.union.entity.UnionLinkConfig;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionLinkConfigParam
 * @Description 转链配置表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionLinkConfig.class)
@Schema(description = "转链配置表新增/修改请求参数")
public class UnionLinkConfigParam {

    @Schema(description = "ID（新增时不需要传）")
    private Long id;

    @Schema(description = "配置名称")
    @NotBlank(message = "配置名称不能为空")
    private String configName;

    @Schema(description = "推广者等级（NULL表示全部）")
    private Integer memberLevel;

    @Schema(description = "平台（NULL表示全部）")
    private Integer platform;

    @Schema(description = "类目ID（NULL表示全部）")
    private Long categoryId;

    @Schema(description = "是否生成短链接")
    private Boolean enableShortUrl;

    @Schema(description = "短链接有效期（天数）")
    private Integer shortUrlExpireDays;

    @Schema(description = "是否生成口令/二维码")
    private Boolean enablePromotionCode;

    @Schema(description = "是否生成海报")
    private Boolean enablePoster;

    @Schema(description = "转链时自动保存商品信息到union_product")
    private Boolean autoSaveProduct;

    @Schema(description = "是否启用点击追踪")
    private Boolean clickTrackingEnabled;

    @Schema(description = "是否启用智能跳转（根据设备跳转不同端）")
    private Boolean smartRedirect;

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
