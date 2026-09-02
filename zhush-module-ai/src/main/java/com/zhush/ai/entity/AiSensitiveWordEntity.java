package com.zhush.ai.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 敏感词库实体（ai_sensitive_word）
 *
 * <p>审批门禁分级命中：1-提示 2-审批 3-拦截，匹配对象支持工具名/入参内容。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("ai_sensitive_word")
public class AiSensitiveWordEntity extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 敏感词 */
    private String word;

    /** 匹配对象：tool_name-工具名 content-入参/内容 both-两者 */
    private String matchType;

    /** 命中级别：1-提示 2-审批 3-拦截 */
    private Integer level;

    /** 业务分组（默认 common） */
    private String groupCode;

    /** 状态：0-停用 1-启用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
