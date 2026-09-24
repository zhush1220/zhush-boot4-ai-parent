package com.zhush.ai.generation.provider;

import com.zhush.common.exception.ServiceExceptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.zhush.ai.enums.AiErrorCode.GENERATION_MODEL_UNAVAILABLE;

/**
 * 生成供应商注册表：按 {@code ai_model.type} 选择 {@link GenerationProvider} 实现。
 *
 * <p>一期只注册 {@link DashScopeGenerationProvider}；新增供应商只需再实现 {@link GenerationProvider}
 * 并声明为 Bean，本注册表自动收集（业务零改动）。未登记的 type 直接报「无可用生成模型」。
 *
 * @author zhush
 */
@Component
public class GenerationProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(GenerationProviderRegistry.class);

    /** 默认供应商（ai_model.type 缺省时回退） */
    private static final String DEFAULT_PROVIDER = "dashscope";

    private final Map<String, GenerationProvider> providers = new HashMap<>();

    public GenerationProviderRegistry(List<GenerationProvider> providerList) {
        for (GenerationProvider provider : providerList) {
            providers.put(provider.name().toLowerCase(), provider);
        }
        log.info("生成供应商注册完成: {}", providers.keySet());
    }

    /**
     * 按供应商标识取实现；{@code provider} 为空回退默认，未知标识抛业务异常。
     */
    public GenerationProvider get(String provider) {
        String key = (provider == null || provider.isBlank()) ? DEFAULT_PROVIDER : provider.toLowerCase();
        GenerationProvider impl = providers.get(key);
        if (impl == null) {
            throw ServiceExceptionUtil.exception(GENERATION_MODEL_UNAVAILABLE);
        }
        return impl;
    }

    /**
     * 默认供应商标识。
     */
    public String defaultProvider() {
        return DEFAULT_PROVIDER;
    }
}
