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
     * 总 Token 数（优先取供应商回传值，缺失时为输入+输出）
     */
    private int totalTokens;

    /**
     * 命中缓存的 Token 数（单独留存，不参与输入+输出合计）
     */
    private int cachedTokens;

    /**
     * 本次会话使用的模型编码（成本定价依据，对应 {@code ai_model.code}）
     */
    private String model;

    /**
     * 耗时（毫秒）
     */
    private long durationMillis;
}
