package com.zhush.agentscope.token;

import io.agentscope.core.model.ChatUsage;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Token 用量累加器：聚合单次调用内多次模型回传的 {@link ChatUsage}。
 *
 * <p>多步 ReAct（推理→工具→再推理）会产生多个 {@code MODEL_CALL_END} 事件，必须累加而非覆盖；
 * 实例随一次 {@code stream()/chat()} 调用创建，由调用链上的各出口共享读取。
 *
 * <p>计数口径：{@code total} 优先取供应商回传的权威值（缺失时退化为输入+输出），
 * {@code cached} 单独留存不参与合计；每累加一次即视为一步推理（{@code stepCount}）。
 *
 * @author zhush
 */
public class TokenUsageAccumulator {

    private final AtomicInteger inputTokens = new AtomicInteger();
    private final AtomicInteger outputTokens = new AtomicInteger();
    private final AtomicInteger cachedTokens = new AtomicInteger();
    private final AtomicInteger totalTokens = new AtomicInteger();
    private final AtomicInteger steps = new AtomicInteger();

    /**
     * 累加一次模型调用的用量（null 安全：部分本地模型不回填 usage）。
     *
     * @param usage 模型回传的用量，可为 null
     */
    public void accumulate(ChatUsage usage) {
        steps.incrementAndGet();
        if (usage == null) {
            return;
        }
        inputTokens.addAndGet(Math.max(usage.getInputTokens(), 0));
        outputTokens.addAndGet(Math.max(usage.getOutputTokens(), 0));
        cachedTokens.addAndGet(Math.max(usage.getCachedTokens(), 0));
        totalTokens.addAndGet(Math.max(usage.getTotalTokens(), 0));
    }

    /**
     * 是否采集到任何用量（false 时落库记 0，不做估算）。
     */
    public boolean isEmpty() {
        return total() == 0;
    }

    public int getStepCount() {
        return steps.get();
    }

    public int getInputTokens() {
        return inputTokens.get();
    }

    public int getOutputTokens() {
        return outputTokens.get();
    }

    public int getCachedTokens() {
        return cachedTokens.get();
    }

    /**
     * 总 Token 数：优先取供应商权威值，缺失时按输入+输出兜底。
     */
    public int getTotalTokens() {
        int total = total();
        return total > 0 ? total : getInputTokens() + getOutputTokens();
    }

    private int total() {
        return totalTokens.get();
    }
}
