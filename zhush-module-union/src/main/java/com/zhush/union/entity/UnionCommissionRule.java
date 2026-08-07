package com.zhush.union.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @ClassName UnionCommissionRule
 * @Description 佣金分配规则配置表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_commission_rule")
public class UnionCommissionRule implements Serializable {

    @Serial
    private static final long serialVersionUID = -7705791703462723753L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则类型：1-全局默认 2-按商品类目 3-按推广者等级 4-按商品平台
     */
    private Integer ruleType;

    /**
     * 平台：1-淘宝 2-京东 3-拼多多（NULL表示全部）
     */
    private Integer platform;

    /**
     * 类目ID（NULL表示全部）
     */
    private Long categoryId;

    /**
     * 推广者等级（NULL表示全部）
     */
    private Integer memberLevel;

    /**
     * 平台服务费率（如淘宝10%）
     */
    private BigDecimal platformServiceRate;

    /**
     * 系统运营成本率（如15%）
     */
    private BigDecimal systemCostRate;

    /**
     * 可分配佣金率 = 1 - platform_service_rate - system_cost_rate
     */
    private BigDecimal distributableRate;

    /**
     * 推广者直接佣金率（占可分配池）
     */
    private BigDecimal directCommissionRate;

    /**
     * 外部推广者佣金比例
     */
    private BigDecimal externalPromoterRate;

    /**
     * 一级上级奖励率（占可分配池）
     */
    private BigDecimal level1Rate;

    /**
     * 二级上级奖励率（占可分配池）
     */
    private BigDecimal level2Rate;

    /**
     * 三级上级奖励率（占可分配池）
     */
    private BigDecimal level3Rate;

    /**
     * 四级上级奖励率
     */
    private BigDecimal level4Rate;

    /**
     * 五级上级奖励率
     */
    private BigDecimal level5Rate;

    /**
     * 优先级（数字越小越优先匹配）
     */
    private Integer priority;

    /**
     * 1-启用 0-禁用
     */
    private Integer status;

    /**
     * 生效开始时间
     */
    private LocalDateTime startTime;

    /**
     * 生效结束时间
     */
    private LocalDateTime endTime;

    /**
     * remark
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
