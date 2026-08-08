package com.zhush.union.param;

import com.zhush.union.entity.UnionMemberLevel;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionMemberLevelParam
 * @Description 推广者等级配置表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionMemberLevel.class)
@Schema(description = "推广者等级配置表新增/修改请求参数")
public class UnionMemberLevelParam {

    @Schema(description = "ID（新增时不需要传）")
    private Long id;

    @Schema(description = "等级编码：L1/L2/L3")
    @NotBlank(message = "等级编码：L1/L2/L3不能为空")
    private String levelCode;

    @Schema(description = "等级名称：见习/正式/黄金/钻石/合伙人")
    @NotBlank(message = "等级名称：见习/正式/黄金/钻石/合伙人不能为空")
    private String levelName;

    @Schema(description = "晋升所需最小团队人数")
    private Integer minTeamSize;

    @Schema(description = "晋升所需最小团队佣金")
    private BigDecimal minTeamCommission;

    @Schema(description = "佣金系数（等级越高，分配比例越高）")
    private BigDecimal commissionMultiplier;

    @Schema(description = "status")
    private Integer status;

    @Schema(description = "sort_order")
    private Integer sortOrder;

}
