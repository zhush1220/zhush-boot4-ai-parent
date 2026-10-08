package com.zhush.ai.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 意图分类入参（B1：auto 能力下的 Jev 意图路由，先问再发）。
 *
 * @author zhush
 */
@Data
public class AgentIntentParam implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 智能体名（如 ai-chat） */
    @NotBlank(message = "智能体名不能为空")
    @Size(max = 64, message = "智能体名长度超出限制")
    private String agentName;

    /** 业务会话 ID（可为空——首条消息无会话时仅按当前消息分类） */
    @Size(max = 64, message = "会话ID长度超出限制")
    private String sessionId;

    /** 用户输入原文（不含能力指令前缀） */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 8000, message = "消息长度超出限制")
    private String message;
}
