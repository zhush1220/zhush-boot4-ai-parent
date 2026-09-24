package com.zhush.ai.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 配额规则实体（ai_quota_rule）
 *
 * <p>按「用户类型 + 任务类型」限制日/月调用次数，{@code 0} 表示不限制。
 * 管理端通常全 0（不限次），会员端按成本敏感度分级收紧。
 *
 * <p>判定口径见 {@code AiQuotaService}：以 {@code ai_usage_record} 的按人按日聚合计数为准，
 * 本表只存规则不存用量，避免规则改动影响历史用量。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("ai_quota_rule")
public class AiQuotaRuleEntity extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 状态：启用 */
    public static final int STATUS_ENABLED = 1;
    /** 状态：停用 */
    public static final int STATUS_DISABLED = 0;

    /** 用户类型：admin-管理端 member-会员端 */
    private String userType;

    /** 任务类型：chat/copywriting/docx/pdf/parse/image/video */
    private String taskType;

    /** 每日次数上限，0=不限 */
    private Integer dailyLimit;

    /** 每月次数上限，0=不限 */
    private Integer monthlyLimit;

    /** 状态：1-启用 0-停用（停用即不校验该规则） */
    private Integer status;

    /** 备注 */
    private String remark;
}
