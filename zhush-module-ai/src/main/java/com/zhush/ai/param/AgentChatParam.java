package com.zhush.ai.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 对话入参（同步/流式共用）。
 *
 * @author zhush
 */
@Data
public class AgentChatParam implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 智能体名（如 ai-chat） */
    @NotBlank(message = "智能体名不能为空")
    private String agentName;

    /** 业务会话 ID（为空时新建会话） */
    private String sessionId;

    /** 用户输入 */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 8000, message = "消息长度超出限制")
    private String message;
}
