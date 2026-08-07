package com.zhush.union.param;

import com.zhush.union.entity.UnionLevelConfig;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionLevelConfigParam
 * @Description 层级佣金配置表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@AutoMapper(target = UnionLevelConfig.class)
@Schema(description = "层级佣金配置表新增/修改请求参数")
public class UnionLevelConfigParam {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "层级（1/2/3/...）")
    private Integer level;

    @Schema(description = "层级名称（如：合伙人/总监/经理）")
    private String levelName;

    @Schema(description = "该层级的佣金系数（如0.1表示10%）")
    private BigDecimal commissionRatio;

    @Schema(description = "晋升该层级所需最小团队人数")
    private Integer minTeamSize;

    @Schema(description = "晋升该层级所需最小团队佣金")
    private BigDecimal minTeamCommission;

    @Schema(description = "是否启用")
    private Integer isActive;

    @Schema(description = "排序")
    private Integer sortOrder;

}
