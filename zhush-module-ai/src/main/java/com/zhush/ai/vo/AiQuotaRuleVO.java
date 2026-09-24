package com.zhush.ai.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 配额规则视图对象。
 *
 * @author zhush
 */
@Data
public class AiQuotaRuleVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    /** 用户类型：admin / member */
    private String userType;

    /** 任务类型：chat/copywriting/docx/pdf/parse/image/video */
    private String taskType;

    /** 任务类型展示名（AiTaskType.desc） */
    private String taskTypeName;

    /** 每日次数上限，0=不限 */
    private Integer dailyLimit;

    /** 每月次数上限，0=不限 */
    private Integer monthlyLimit;

    /** 状态：0-停用 1-启用 */
    private Integer status;

    /** 备注 */
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
