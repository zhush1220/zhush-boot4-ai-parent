package com.zhush.union.param;

import com.zhush.union.entity.UnionCommissionRule;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionCommissionRuleParam
 * @Description 佣金分配规则配置表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@AutoMapper(target = UnionCommissionRule.class)
@Schema(description = "佣金分配规则配置表新增/修改请求参数")
public class UnionCommissionRuleParam {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "规则类型：1-全局默认 2-按商品类目 3-按推广者等级 4-按商品平台")
    private Integer ruleType;

    @Schema(description = "平台：1-淘宝 2-京东 3-拼多多（NULL表示全部）")
    private Integer platform;

    @Schema(description = "类目ID（NULL表示全部）")
    private Long categoryId;

    @Schema(description = "推广者等级（NULL表示全部）")
    private Integer memberLevel;

    @Schema(description = "平台服务费率（如淘宝10%）")
    private BigDecimal platformServiceRate;

    @Schema(description = "系统运营成本率（如15%）")
    private BigDecimal systemCostRate;

    @Schema(description = "可分配佣金率 = 1 - platform_service_rate - system_cost_rate")
    private BigDecimal distributableRate;

    @Schema(description = "推广者直接佣金率（占可分配池）")
    private BigDecimal directCommissionRate;

    @Schema(description = "外部推广者佣金比例")
    private BigDecimal externalPromoterRate;

    @Schema(description = "一级上级奖励率（占可分配池）")
    private BigDecimal level1Rate;

    @Schema(description = "二级上级奖励率（占可分配池）")
    private BigDecimal level2Rate;

    @Schema(description = "三级上级奖励率（占可分配池）")
    private BigDecimal level3Rate;

    @Schema(description = "四级上级奖励率")
    private BigDecimal level4Rate;

    @Schema(description = "五级上级奖励率")
    private BigDecimal level5Rate;

    @Schema(description = "优先级（数字越小越优先匹配）")
    private Integer priority;

    @Schema(description = "1-启用 0-禁用")
    private Integer status;

    @Schema(description = "生效开始时间")
    private LocalDateTime startTime;

    @Schema(description = "生效结束时间")
    private LocalDateTime endTime;

    @Schema(description = "remark")
    private String remark;

}
