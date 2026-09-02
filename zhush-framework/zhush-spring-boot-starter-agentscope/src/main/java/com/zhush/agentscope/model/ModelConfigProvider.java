package com.zhush.agentscope.model;

import java.util.List;

/**
 * 动态模型配置提供者 SPI：业务模块（如 zhush-module-ai）实现，向框架输出启用态模型。
 *
 * <p>无实现时框架回退 {@code zhush.agentscope.models} yaml 配置。
 *
 * @author zhush
 */
public interface ModelConfigProvider {

    /**
     * 返回当前启用态模型配置（建议按优先级排序）。
     */
    List<ModelConfig> enabledModels();
}
