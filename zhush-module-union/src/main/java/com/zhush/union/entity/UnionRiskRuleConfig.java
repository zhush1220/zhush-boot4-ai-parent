package com.zhush.union.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @ClassName UnionRiskRuleConfig
 * @Description 风控规则配置表 实体类
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@Accessors(chain = true)
@Table(value = "union_risk_rule_config")
public class UnionRiskRuleConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = -7007401436961121474L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 规则编码（唯一标识）
     */
    private String ruleCode;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则分类：1-用户行为 2-账户安全 3-交易异常 4-设备环境 5-业务合规
     */
    private Integer ruleCategory;

    /**
     * 规则类型：1-硬拒绝（直接拒绝） 2-软拒绝（转人工） 3-降级（限制额度） 4-仅记录（不打分）
     */
    private Integer ruleType;

    /**
     * condition_expression
     */
    private String conditionExpression;

    /**
     * 条件SQL片段（预编译，用于数据库过滤）
     */
    private String conditionSql;

    /**
     * 动作类型：1-拒绝 2-转人工审核 3-限制提现额度 4-冻结账户 5-发送告警
     */
    private Integer actionType;

    /**
     * action_params
     */
    private String actionParams;

    /**
     * 命中的风险分值（累加）
     */
    private Integer riskScore;

    /**
     * 自动审核通过阈值（低于此值自动通过）
     */
    private Integer autoAuditThreshold;

    /**
     * 执行优先级（数字越小越先执行）
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
     * 规则描述
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
