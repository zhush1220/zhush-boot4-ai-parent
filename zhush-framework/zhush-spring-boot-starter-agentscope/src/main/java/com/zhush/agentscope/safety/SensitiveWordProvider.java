package com.zhush.agentscope.safety;

import java.util.List;

/**
 * 动态敏感词提供者 SPI：业务模块（如 zhush-module-ai）实现，向框架输出启用态敏感词。
 *
 * <p>无实现或返回空时框架回退 {@code zhush.agentscope.safety.sensitive-keywords} yaml 配置。
 *
 * @author zhush
 */
public interface SensitiveWordProvider {

    /**
     * 返回当前启用态敏感词。
     */
    List<SensitiveWordConfig> enabledWords();
}
