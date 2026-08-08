package com.zhush.union.param;

import com.zhush.union.entity.UnionLevelConfig;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionLevelConfigParam
 * @Description 层级佣金配置表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionLevelConfig.class)
@Schema(description = "层级佣金配置表新增/修改请求参数")
public class UnionLevelConfigParam {

    @Schema(description = "ID（新增时不需要传）")
    private Long id;

    @Schema(description = "层级（1/2/3/...）")
    @NotNull(message = "层级（1/2/3/...）不能为空")
    private Integer level;

    @Schema(description = "层级名称（如：合伙人/总监/经理）")
    @NotBlank(message = "层级名称（如：合伙人/总监/经理）不能为空")
    private String levelName;

    @Schema(description = "该层级的佣金系数（如0.1表示10%）")
    @NotNull(message = "该层级的佣金系数（如0.1表示10%）不能为空")
    private BigDecimal commissionRatio;

    @Schema(description = "晋升该层级所需最小团队人数")
    private Integer minTeamSize;

    @Schema(description = "晋升该层级所需最小团队佣金")
    private BigDecimal minTeamCommission;

    @Schema(description = "是否启用")
    private Boolean isActive;

    @Schema(description = "排序")
    private Integer sortOrder;

}
