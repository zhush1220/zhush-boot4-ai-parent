package com.zhush.ai.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 敏感词保存入参（管理端新增/编辑）。
 *
 * @author zhush
 */
@Data
public class AiSensitiveWordParam implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（编辑时必填） */
    private Long id;

    /** 敏感词 */
    @NotBlank(message = "敏感词不能为空")
    private String word;

    /** 匹配对象：tool_name / content / both */
    private String matchType;

    /** 命中级别：1-提示 2-审批 3-拦截 */
    @NotNull(message = "命中级别不能为空")
    private Integer level;

    /** 业务分组 */
    private String groupCode;

    /** 状态：0-停用 1-启用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
