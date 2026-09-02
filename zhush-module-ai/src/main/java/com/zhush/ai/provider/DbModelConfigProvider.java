package com.zhush.ai.provider;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.zhush.agentscope.model.ModelConfig;
import com.zhush.agentscope.model.ModelConfigProvider;
import com.zhush.ai.entity.AiModelEntity;
import com.zhush.ai.service.AiModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 动态模型配置提供者（DB 实现）：向框架输出 {@code ai_model} 启用态模型。
 *
 * @author zhush
 */
@Component
public class DbModelConfigProvider implements ModelConfigProvider {

    private static final Logger log = LoggerFactory.getLogger(DbModelConfigProvider.class);

    private final AiModelService modelService;
    private final ObjectMapper objectMapper;

    public DbModelConfigProvider(AiModelService modelService, ObjectMapper objectMapper) {
        this.modelService = modelService;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ModelConfig> enabledModels() {
        List<AiModelEntity> entities = modelService.enabledEntities();
        List<ModelConfig> result = new ArrayList<>(entities.size());
        for (AiModelEntity entity : entities) {
            ModelConfig config = new ModelConfig();
            config.setCode(entity.getCode());
            config.setName(entity.getName());
            config.setType(entity.getType());
            config.setApiKey(modelService.resolveApiKey(entity));
            config.setBaseUrl(entity.getBaseUrl());
            config.setModel(entity.getModel());
            config.setCapability(entity.getCapability());
            config.setDefault(entity.getIsDefault() != null && entity.getIsDefault() == 1);
            config.setPriority(entity.getPriority() == null ? 0 : entity.getPriority());
            config.setOptions(parseOptions(entity.getOptions()));
            result.add(config);
        }
        return result;
    }

    private Map<String, Object> parseOptions(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() { });
        } catch (JacksonException e) {
            log.warn("模型透传参数解析失败, json={}", json);
            return null;
        }
    }
}
