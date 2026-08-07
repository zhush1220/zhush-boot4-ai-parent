package com.zhush.union.vo;

import com.zhush.union.entity.UnionRiskRuleConfig;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionRiskRuleConfigVO
 * @Description 风控规则配置表 详情/列表返回
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@AutoMapper(target = UnionRiskRuleConfig.class)
@Schema(description = "风控规则配置表详情/列表返回")
public class UnionRiskRuleConfigVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "规则编码（唯一标识）")
    private String ruleCode;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "规则分类：1-用户行为 2-账户安全 3-交易异常 4-设备环境 5-业务合规")
    private Integer ruleCategory;

    @Schema(description = "规则类型：1-硬拒绝（直接拒绝） 2-软拒绝（转人工） 3-降级（限制额度） 4-仅记录（不打分）")
    private Integer ruleType;

    @Schema(description = "condition_expression")
    private String conditionExpression;

    @Schema(description = "条件SQL片段（预编译，用于数据库过滤）")
    private String conditionSql;

    @Schema(description = "动作类型：1-拒绝 2-转人工审核 3-限制提现额度 4-冻结账户 5-发送告警")
    private Integer actionType;

    @Schema(description = "action_params")
    private String actionParams;

    @Schema(description = "命中的风险分值（累加）")
    private Integer riskScore;

    @Schema(description = "自动审核通过阈值（低于此值自动通过）")
    private Integer autoAuditThreshold;

    @Schema(description = "执行优先级（数字越小越先执行）")
    private Integer priority;

    @Schema(description = "1-启用 0-禁用")
    private Integer status;

    @Schema(description = "生效开始时间")
    private LocalDateTime startTime;

    @Schema(description = "生效结束时间")
    private LocalDateTime endTime;

    @Schema(description = "规则描述")
    private String description;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
