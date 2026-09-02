package com.zhush.ai.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 敏感词视图对象。
 *
 * @author zhush
 */
@Data
public class AiSensitiveWordVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    /** 敏感词 */
    private String word;

    /** 匹配对象：tool_name / content / both */
    private String matchType;

    /** 命中级别：1-提示 2-审批 3-拦截 */
    private Integer level;

    /** 业务分组 */
    private String groupCode;

    /** 状态：0-停用 1-启用 */
    private Integer status;

    /** 备注 */
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
