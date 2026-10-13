package com.zhush.ai.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 输入优化（润色）入参：对输入框草稿做一次一次性大模型改写，不落会话、不计对话配额。
 *
 * @author zhush
 */
@Data
public class AgentPolishParam implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 智能体名（可空，缺省 ai-chat；决定润色所用的模型与系统提示） */
    @Size(max = 64, message = "智能体名长度超出限制")
    private String agentName;

    /** 待优化原文 */
    @NotBlank(message = "待优化内容不能为空")
    @Size(max = 8000, message = "内容长度超出限制")
    private String text;

    /** 风格倾向（可空）：简洁 / 专业 / 亲和 / 正式；缺省为「更清晰、通顺、专业」 */
    @Size(max = 32, message = "风格描述长度超出限制")
    private String style;

    /** 当前能力/任务上下文（可空，如 copywriting/docx）：润色时保持该任务意图不变 */
    @Size(max = 32, message = "任务类型长度超出限制")
    private String taskType;
}
