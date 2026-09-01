package com.zhush.agentscope.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 会话内单条消息（用户 / 助手 / 工具）。
 *
 * @author zhush
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 角色：user / assistant / tool
     */
    private String role;

    /**
     * 消息内容（assistant 可为最终文本；tool 为结果）
     */
    private String content;

    /**
     * 工具名（role=tool 时）
     */
    private String toolName;

    /**
     * 是否流式中（assistant 增量标记）
     */
    private boolean streaming;
}
