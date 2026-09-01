package com.zhush.agentscope.model;

import com.zhush.agentscope.config.AgentScopeProperties;
import io.agentscope.core.model.Model;

/**
 * 模型供应商 SPI：新增供应商只需实现并注册为 Spring Bean，框架按 type 分发。
 *
 * <p>可扩展点 1：接入新大模型服务商（如通义、OpenAI、本地 Ollama、甚至自建网关）。
 *
 * @author zhush
 */
public interface ChatModelFactory {

    /**
     * 供应商标识（type）：dashscope / openai / ollama。
     *
     * @return 供应商标识
     */
    String type();

    /**
     * 依据配置创建 {@link Model}。
     *
     * @param props 模型配置
     * @return 模型实例
     */
    Model create(AgentScopeProperties.ModelProperties props);
}
