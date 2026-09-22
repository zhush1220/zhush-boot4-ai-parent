package com.zhush.agentscope.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 一次对话的最终回复（同步 / 异步门面返回）
 *
 * @author zhush
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentReply implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会话 ID（新建时由 AgentTemplate 生成返回）
     */
    private String sessionId;

    /**
     * 智能体名
     */
    private String agentName;

    /**
     * 助手回复文本
     */
    private String reply;

    /**
     * 是否完成（false 表示被审批/中断挂起）
     */
    private boolean finished;

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
     * 本次使用的模型编码（成本定价依据）
     */
    private String model;

    /**
     * 耗时（毫秒）
     */
    private long durationMillis;
}
