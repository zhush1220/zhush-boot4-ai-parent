package com.zhush.agentscope.model;

import com.zhush.agentscope.config.AgentScopeProperties;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ModelCreationContext;
import io.agentscope.extensions.model.dashscope.DashScopeModelProvider;
import io.agentscope.extensions.model.ollama.OllamaModelProvider;
import io.agentscope.extensions.model.openai.OpenAIModelProvider;

/**
 * 默认模型工厂：按配置的 {@code type} 分发到内置供应商（DashScope / OpenAI / Ollama）。
 *
 * <p>模型供应商为 AgentScope 的 {@code ModelProvider} SPI（通过扩展模块 jar 提供），
 * 新增供应商仅需实现 {@link ChatModelFactory} 并注册 Spring Bean。
 *
 * @author zhush
 */
public class DefaultModelFactory implements ChatModelFactory {

    private final String defaultModel;

    public DefaultModelFactory(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    @Override
    public String type() {
        return "default";
    }

    @Override
    public Model create(AgentScopeProperties.ModelProperties props) {
        String providerType = props.getType();
        String model = (props.getModel() == null || props.getModel().isEmpty())
                ? defaultModel : props.getModel();
        ModelCreationContext context = toCreationContext(props);
        return switch (providerType) {
            case "dashscope" -> new DashScopeModelProvider().create(qualified(model, "dashscope"), context);
            case "openai" -> new OpenAIModelProvider().create(qualified(model, "openai"), context);
            case "ollama" -> new OllamaModelProvider().create(qualified(model, "ollama"), context);
            default -> throw new UnsupportedOperationException("暂不支持的模型供应商类型：" + providerType);
        };
    }

    /**
     * 为模型名补充供应商前缀（如 openai:deepseek-chat），满足各供应商 {@code supports} 的格式校验；
     * 供应商 create 内部会剥离前缀后使用真实模型名。已带同前缀时不重复添加。
     */
    private String qualified(String model, String type) {
        if (model == null || model.isEmpty()) {
            return model;
        }
        return model.startsWith(type + ":") ? model : type + ":" + model;
    }

    /**
     * 将模型配置转换为供应商创建上下文（传递 apiKey / baseUrl / 透传参数）。
     * apiKey 为空时不设置，交由供应商回退环境变量读取。
     */
    private ModelCreationContext toCreationContext(AgentScopeProperties.ModelProperties props) {
        ModelCreationContext.Builder builder = ModelCreationContext.builder();
        if (props.getApiKey() != null && !props.getApiKey().isBlank()) {
            builder.apiKey(props.getApiKey());
        }
        if (props.getBaseUrl() != null && !props.getBaseUrl().isBlank()) {
            builder.baseUrl(props.getBaseUrl());
        }
        if (props.getOptions() != null && !props.getOptions().isEmpty()) {
            props.getOptions().forEach(builder::option);
        }
        return builder.build();
    }

    /**
     * 按逻辑模型名（type:model）解析模型；type 未指定时使用默认 type。
     *
     * @param modelName 模型名（如 qwen-max，可为 "dashscope:qwen-max" 形式）
     * @return 模型实例
     */
    public Model resolve(String modelName) {
        String type = "dashscope";
        String model = modelName;
        int colon = modelName == null ? -1 : modelName.indexOf(':');
        if (colon > 0) {
            type = modelName.substring(0, colon);
            model = modelName.substring(colon + 1);
        }
        AgentScopeProperties.ModelProperties props = new AgentScopeProperties.ModelProperties();
        props.setType(type);
        props.setModel(model);
        return create(props);
    }
}
