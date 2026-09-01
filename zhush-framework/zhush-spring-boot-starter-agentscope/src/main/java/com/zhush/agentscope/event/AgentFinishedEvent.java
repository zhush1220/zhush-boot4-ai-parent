package com.zhush.agentscope.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * 会话结束事件（含最终回复 / 耗时 / Token 消耗 / 步数）。
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class AgentFinishedEvent extends AgentEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 最终回复文本
     */
    private String reply;

    /**
     * 推理步数
     */
    private int stepCount;

    /**
     * 输入 Token 数
     */
    private int inputTokens;

    /**
     * 输出 Token 数
     */
    private int outputTokens;

    /**
     * 耗时（毫秒）
     */
    private long durationMillis;
}
