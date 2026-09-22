package com.zhush.agentscope.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 模型单价（成本估算用）：来自 {@code ai_model.input_price / output_price}，单位「元 / 千 token」。
 *
 * <p>缺价（null 或 0）时成本记 0，即「只计量不计费」，不阻断主流程。
 * 结果保留 4 位小数，与 {@code agent_token_usage.cost DECIMAL(12,4)} 对齐。
 *
 * @author zhush
 */
public class ModelPricing {

    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000L);

    /**
     * 成本保留位数（对齐 cost 列精度）
     */
    public static final int COST_SCALE = 4;

    private final BigDecimal inputPrice;
    private final BigDecimal outputPrice;

    public ModelPricing(BigDecimal inputPrice, BigDecimal outputPrice) {
        this.inputPrice = inputPrice;
        this.outputPrice = outputPrice;
    }

    /**
     * 是否配置了有效单价（全为 0/null 视为未定价）。
     */
    public boolean hasPrice() {
        return isPositive(inputPrice) || isPositive(outputPrice);
    }

    public BigDecimal getInputPrice() {
        return inputPrice;
    }

    public BigDecimal getOutputPrice() {
        return outputPrice;
    }

    /**
     * 按用量折算成本：{@code cost = in/1000*inputPrice + out/1000*outputPrice}。
     *
     * @param inputTokens  输入 Token 数
     * @param outputTokens 输出 Token 数
     * @return 成本（元，4 位小数），未定价返回 0
     */
    public BigDecimal calculate(int inputTokens, int outputTokens) {
        if (!hasPrice()) {
            return BigDecimal.ZERO.setScale(COST_SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal cost = zeroIfNull(inputPrice)
                .multiply(BigDecimal.valueOf(Math.max(inputTokens, 0)))
                .divide(THOUSAND, 10, RoundingMode.HALF_UP)
                .add(zeroIfNull(outputPrice)
                        .multiply(BigDecimal.valueOf(Math.max(outputTokens, 0)))
                        .divide(THOUSAND, 10, RoundingMode.HALF_UP));
        return cost.setScale(COST_SCALE, RoundingMode.HALF_UP);
    }

    private static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
