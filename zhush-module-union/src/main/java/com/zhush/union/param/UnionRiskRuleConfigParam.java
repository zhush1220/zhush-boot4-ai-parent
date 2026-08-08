package com.zhush.union.param;

import com.zhush.union.entity.UnionRiskRuleConfig;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionRiskRuleConfigParam
 * @Description 风控规则配置表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/08
 **/
@Data
@AutoMapper(target = UnionRiskRuleConfig.class)
@Schema(description = "风控规则配置表新增/修改请求参数")
public class UnionRiskRuleConfigParam {

    @Schema(description = "ID（新增时不需要传）")
    private Long id;

    @Schema(description = "规则编码（唯一标识）")
    @NotBlank(message = "规则编码（唯一标识）不能为空")
    private String ruleCode;

    @Schema(description = "规则名称")
    @NotBlank(message = "规则名称不能为空")
    private String ruleName;

    @Schema(description = "规则分类：1-用户行为 2-账户安全 3-交易异常 4-设备环境 5-业务合规")
    @NotNull(message = "规则分类：1-用户行为 2-账户安全 3-交易异常 4-设备环境 5-业务合规不能为空")
    private Integer ruleCategory;

    @Schema(description = "规则类型：1-硬拒绝（直接拒绝） 2-软拒绝（转人工） 3-降级（限制额度） 4-仅记录（不打分）")
    @NotNull(message = "规则类型：1-硬拒绝（直接拒绝） 2-软拒绝（转人工） 3-降级（限制额度） 4-仅记录（不打分）不能为空")
    private Integer ruleType;

    @Schema(description = "condition_expression")
    @NotBlank(message = "condition_expression不能为空")
    private String conditionExpression;

    @Schema(description = "条件SQL片段（预编译，用于数据库过滤）")
    private String conditionSql;

    @Schema(description = "动作类型：1-拒绝 2-转人工审核 3-限制提现额度 4-冻结账户 5-发送告警")
    @NotNull(message = "动作类型：1-拒绝 2-转人工审核 3-限制提现额度 4-冻结账户 5-发送告警不能为空")
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

}
