package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionCommissionRuleQuery
 * @Description 佣金分配规则配置表 分页查询请求
 * @Author zhush
 * @Date 2026/08/07
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "佣金分配规则配置表分页查询请求")
public class UnionCommissionRuleQuery extends BaseQuery {

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

    @Schema(description = "优先级（数字越小越优先匹配）")
    private Integer priority;

    @Schema(description = "1-启用 0-禁用")
    private Integer status;

    @Schema(description = "remark")
    private String remark;

}
