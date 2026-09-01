package com.zhush.agentscope.model;

import com.zhush.agentscope.config.AgentScopeProperties;
import io.agentscope.core.model.Model;
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
        return switch (providerType) {
            case "dashscope" -> new DashScopeModelProvider().create(model);
            case "openai" -> new OpenAIModelProvider().create(model);
            case "ollama" -> new OllamaModelProvider().create(model);
            default -> throw new UnsupportedOperationException("暂不支持的模型供应商类型：" + providerType);
        };
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
