package com.zhush.agentscope.model;

import com.zhush.agentscope.config.AgentScopeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 动态模型解析器：缓存启用态模型池（由 {@link ModelConfigProvider} 加载），
 * 按模型编码解析为框架可消费的 {@link AgentScopeProperties.ModelProperties}。
 *
 * <p>读取顺序：动态模型池（优先） → yaml {@code zhush.agentscope.models}（回退）。
 *
 * @author zhush
 */
public class AgentModelResolver {

    private static final Logger log = LoggerFactory.getLogger(AgentModelResolver.class);

    private final List<ModelConfigProvider> providers;
    private final AgentScopeProperties properties;

    /** 启用态模型池缓存（code -> config），volatile 整体替换保证读一致性 */
    private volatile Map<String, ModelConfig> cache = Collections.emptyMap();

    public AgentModelResolver(List<ModelConfigProvider> providers, AgentScopeProperties properties) {
        this.providers = providers;
        this.properties = properties;
        this.refresh();
    }

    /**
     * 重新从 Provider 加载启用态模型（变更/广播/定时触发）。
     */
    public synchronized void refresh() {
        Map<String, ModelConfig> next = new HashMap<>();
        for (ModelConfigProvider provider : providers) {
            try {
                List<ModelConfig> models = provider.enabledModels();
                if (models != null) {
                    for (ModelConfig config : models) {
                        if (config != null && config.getCode() != null) {
                            next.put(config.getCode(), config);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("动态模型加载失败, provider={}", provider.getClass().getSimpleName(), e);
            }
        }
        this.cache = next;
        log.info("动态模型池刷新完成, 启用模型数={}", next.size());
    }

    /**
     * 按模型编码解析模型属性（动态优先，回退 yaml）。
     *
     * @param modelCode 模型编码（如 qwen-max）
     * @return 模型属性，均未命中时返回 null
     */
    public AgentScopeProperties.ModelProperties resolve(String modelCode) {
        if (modelCode == null || modelCode.isEmpty()) {
            return null;
        }
        ModelConfig config = cache.get(modelCode);
        if (config != null) {
            return toProperties(config);
        }
        // 回退 yaml 配置
        return properties.getModels().get(modelCode);
    }

    /**
     * 当前启用态模型池快照（只读，供管理端下拉等使用）。
     */
    public Map<String, ModelConfig> enabledModels() {
        return Collections.unmodifiableMap(cache);
    }

    /**
     * 解析运行时默认模型编码：动态池默认（is_default=1 且启用）优先，回退 yaml default-model。
     *
     * @return 默认模型编码，未配置时返回 null
     */
    public String resolveDefaultCode() {
        String fallback = properties.getDefaultModel();
        for (ModelConfig config : cache.values()) {
            if (config.isDefault()) {
                return config.getCode();
            }
        }
        return fallback;
    }

    /**
     * 按模型编码解析单价（仅动态模型池携带单价，yaml 回退路径无单价）。
     *
     * @param modelCode 模型编码
     * @return 单价；未配置有效单价时返回 null（调用方按 0 成本处理）
     */
    public ModelPricing resolvePricing(String modelCode) {
        if (modelCode == null || modelCode.isEmpty()) {
            return null;
        }
        ModelConfig config = cache.get(modelCode);
        return config == null ? null : config.pricing();
    }

    /**
     * 按能力类型解析启用态模型（图像/视频生成取凭证用）：同能力多模型时取 {@code priority} 最大者。
     *
     * <p>仅覆盖动态模型池（启用态），不回退 yaml——生成类能力必须由显式配置的供应商模型承担，
     * 避免误用 yaml 里的对话模型凭证。无匹配时返回 {@link Optional#empty()}，调用方据此报「无可用模型」。
     *
     * @param capability 能力类型：image / video（对应 {@code ai_model.capability}）
     * @return 命中的模型配置（含明文 apiKey），无则空
     */
    public Optional<ModelConfig> resolveByCapability(String capability) {
        if (capability == null || capability.isEmpty()) {
            return Optional.empty();
        }
        ModelConfig best = null;
        for (ModelConfig config : cache.values()) {
            if (capability.equalsIgnoreCase(config.getCapability())) {
                if (best == null || config.getPriority() > best.getPriority()) {
                    best = config;
                }
            }
        }
        return Optional.ofNullable(best);
    }

    private AgentScopeProperties.ModelProperties toProperties(ModelConfig config) {
        AgentScopeProperties.ModelProperties mp = new AgentScopeProperties.ModelProperties();
        mp.setType(config.getType() == null || config.getType().isEmpty() ? "dashscope" : config.getType());
        mp.setApiKey(config.getApiKey());
        mp.setBaseUrl(config.getBaseUrl());
        mp.setModel(config.getModel());
        if (config.getOptions() != null) {
            mp.setOptions(config.getOptions());
        }
        return mp;
    }
}
