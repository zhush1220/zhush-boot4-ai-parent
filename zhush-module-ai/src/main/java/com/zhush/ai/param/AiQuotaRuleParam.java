package com.zhush.ai.param;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 配额规则保存入参（管理端新增/编辑）。
 *
 * <p>业务合法性（taskType 枚举、userType 白名单、上限非负）由服务层统一校验，
 * 与 {@code AiTaskType}/{@code ai_quota_rule} 落库口径保持一致。
 *
 * @author zhush
 */
@Data
public class AiQuotaRuleParam implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（编辑时必填） */
    private Long id;

    /** 用户类型：admin / member */
    @NotBlank(message = "用户类型不能为空")
    private String userType;

    /** 任务类型：chat/copywriting/docx/pdf/parse/image/video */
    @NotBlank(message = "任务类型不能为空")
    private String taskType;

    /** 每日次数上限，0=不限 */
    private Integer dailyLimit;

    /** 每月次数上限，0=不限 */
    private Integer monthlyLimit;

    /** 状态：0-停用 1-启用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
