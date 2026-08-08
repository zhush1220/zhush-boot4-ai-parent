package com.zhush.union.vo;

import com.zhush.union.entity.UnionMemberLevel;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionMemberLevelVO
 * @Description 推广者等级配置表 详情/列表返回
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionMemberLevel.class)
@Schema(description = "推广者等级配置表详情/列表返回")
public class UnionMemberLevelVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "等级编码：L1/L2/L3")
    private String levelCode;

    @Schema(description = "等级名称：见习/正式/黄金/钻石/合伙人")
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

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
