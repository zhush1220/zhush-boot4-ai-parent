package com.zhush.agentscope.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 动态模型配置（由业务模块通过 {@link ModelConfigProvider} 输出）。
 *
 * @author zhush
 */
public class ModelConfig {

    /** 模型编码（唯一，Agent 定义 model 字段引用） */
    private String code;

    /** 展示名 */
    private String name;

    /** 供应商标识：dashscope / openai / ollama */
    private String type;

    /** API Key（已解密的明文，支持 ${ENV} 占位由 Provider 侧解析） */
    private String apiKey;

    /** 服务地址（openai 兼容服务） */
    private String baseUrl;

    /** 实际模型名（如 qwen-max、wanx2.1-t2i） */
    private String model;

    /** 能力类型：text / image / video / audio */
    private String capability;

    /** 是否默认模型 */
    private boolean isDefault;

    /** 优先级（越大越靠前） */
    private int priority;

    /** 透传参数 */
    private Map<String, Object> options;

    /** 输入单价（元/千 token，空或 0 表示不计价） */
    private BigDecimal inputPrice;

    /** 输出单价（元/千 token，空或 0 表示不计价） */
    private BigDecimal outputPrice;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getCapability() {
        return capability;
    }

    public void setCapability(String capability) {
        this.capability = capability;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }

    public BigDecimal getInputPrice() {
        return inputPrice;
    }

    public void setInputPrice(BigDecimal inputPrice) {
        this.inputPrice = inputPrice;
    }

    public BigDecimal getOutputPrice() {
        return outputPrice;
    }

    public void setOutputPrice(BigDecimal outputPrice) {
        this.outputPrice = outputPrice;
    }

    /**
     * 单价快照（无有效单价时返回 null，调用方按 0 成本处理）。
     */
    public ModelPricing pricing() {
        ModelPricing pricing = new ModelPricing(inputPrice, outputPrice);
        return pricing.hasPrice() ? pricing : null;
    }
}
