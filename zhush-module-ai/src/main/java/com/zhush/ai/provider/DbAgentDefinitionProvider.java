package com.zhush.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.ai.entity.AgentDefinitionEntity;
import com.zhush.ai.mapper.AgentDefinitionMapper;
import com.zhush.agentscope.agent.AgentDefinition;
import com.zhush.agentscope.agent.AgentDefinitionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库智能体定义提供器：从 {@code agent_definition} 读取启用定义，向 AgentRegistry 注册。
 *
 * <p>支持管理端在线配置（source=2）生效；union 等代码声明（source=1）的智能体另由业务模块提供。
 *
 * @author zhush
 */
@Component
public class DbAgentDefinitionProvider implements AgentDefinitionProvider {

    private static final Logger log = LoggerFactory.getLogger(DbAgentDefinitionProvider.class);

    private static final int STATUS_ENABLED = 1;

    private final AgentDefinitionMapper definitionMapper;
    private final ObjectMapper objectMapper;

    public DbAgentDefinitionProvider(AgentDefinitionMapper definitionMapper, ObjectMapper objectMapper) {
        this.definitionMapper = definitionMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<AgentDefinition> getDefinitions() {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(AgentDefinitionEntity::getStatus).eq(STATUS_ENABLED);
        List<AgentDefinitionEntity> entities = definitionMapper.selectListByQuery(queryWrapper);
        if (entities == null) {
            return List.of();
        }
        List<AgentDefinition> result = new ArrayList<>(entities.size());
        for (AgentDefinitionEntity entity : entities) {
            result.add(toDefinition(entity));
        }
        return result;
    }

    private AgentDefinition toDefinition(AgentDefinitionEntity entity) {
        return AgentDefinition.builder()
                .name(entity.getName())
                .description(entity.getDescription())
                .systemPrompt(entity.getSystemPrompt())
                .model(entity.getModel())
                .toolNames(parseList(entity.getToolNames()))
                .streaming(entity.getStreaming() == null || entity.getStreaming() == 1)
                .maxSteps(entity.getMaxSteps() == null ? 6 : entity.getMaxSteps())
                .skills(parseList(entity.getSkills()))
                .groupCode(entity.getGroupCode())
                .build();
    }

    private List<String> parseList(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.warn("解析 JSON 列表失败: {}", e.getMessage());
            return null;
        }
    }
}
