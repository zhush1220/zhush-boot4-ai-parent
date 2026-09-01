package com.zhush.ai.service;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.ai.entity.AgentDefinitionEntity;
import com.zhush.ai.mapper.AgentDefinitionMapper;
import com.zhush.ai.param.AgentDefinitionParam;
import com.zhush.ai.query.AgentDefinitionQuery;
import com.zhush.ai.vo.AgentDefinitionVO;
import com.zhush.ai.vo.AgentModelVO;
import com.zhush.agentscope.agent.AgentDefinition;
import com.zhush.agentscope.agent.AgentRegistry;
import com.zhush.agentscope.config.AgentScopeProperties;
import com.zhush.common.exception.ServiceExceptionUtil;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.zhush.ai.enums.AiErrorCode.CONVERSATION_NOT_FOUND;

/**
 * 智能体定义管理服务：落库 {@code agent_definition} 并同步运行时 {@link AgentRegistry}。
 *
 * <p>管理端在线配置（source=2）保存后立即注册生效；删除/停用同步移除。
 *
 * @author zhush
 */
@Service
public class AgentManageService {

    private static final Logger log = LoggerFactory.getLogger(AgentManageService.class);

    private static final int SOURCE_ADMIN = 2;
    private static final int STATUS_ENABLED = 1;

    private final AgentDefinitionMapper definitionMapper;
    private final AgentRegistry agentRegistry;
    private final ObjectMapper objectMapper;
    private final AgentScopeProperties properties;

    public AgentManageService(AgentDefinitionMapper definitionMapper, AgentRegistry agentRegistry,
                              ObjectMapper objectMapper, AgentScopeProperties properties) {
        this.definitionMapper = definitionMapper;
        this.agentRegistry = agentRegistry;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * 分页查询。
     */
    public PageResult<AgentDefinitionVO> page(AgentDefinitionQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(AgentDefinitionEntity::getName, query.getName())
                .eq(AgentDefinitionEntity::getModel, query.getModel())
                .eq(AgentDefinitionEntity::getStreaming, query.getStreaming())
                .eq(AgentDefinitionEntity::getGroupCode, query.getGroupCode())
                .eq(AgentDefinitionEntity::getStatus, query.getStatus());
        queryWrapper.orderBy("create_time", false);
        Page<AgentDefinitionEntity> page = definitionMapper.selectPageX(query, queryWrapper);
        List<AgentDefinitionVO> list = new ArrayList<>(page.getRecords().size());
        for (AgentDefinitionEntity entity : page.getRecords()) {
            list.add(toVO(entity));
        }
        return PageResult.of(list, page.getTotalRow());
    }

    /**
     * 详情。
     */
    public AgentDefinitionVO get(Long id) {
        AgentDefinitionEntity entity = definitionMapper.selectOneById(id);
        ServiceExceptionUtil.assertNotNull(entity, CONVERSATION_NOT_FOUND, id);
        return toVO(entity);
    }

    /**
     * 新增（落库 + 注册运行态）。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(AgentDefinitionParam param) {
        AgentDefinitionEntity entity = new AgentDefinitionEntity();
        apply(entity, param);
        entity.setSource(SOURCE_ADMIN);
        entity.setVersion(1);
        if (entity.getStatus() == null) {
            entity.setStatus(STATUS_ENABLED);
        }
        definitionMapper.insert(entity);
        refreshRuntime(entity);
        return entity.getId();
    }

    /**
     * 编辑（落库 + 刷新运行态）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(AgentDefinitionParam param) {
        AgentDefinitionEntity entity = definitionMapper.selectOneById(param.getId());
        ServiceExceptionUtil.assertNotNull(entity, CONVERSATION_NOT_FOUND, param.getId());
        apply(entity, param);
        entity.setVersion(entity.getVersion() == null ? 1 : entity.getVersion() + 1);
        definitionMapper.update(entity);
        refreshRuntime(entity);
    }

    /**
     * 删除（落库 + 移除运行态）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AgentDefinitionEntity entity = definitionMapper.selectOneById(id);
        if (entity == null) {
            return;
        }
        definitionMapper.deleteById(id);
        agentRegistry.remove(entity.getName());
    }

    /**
     * 模型列表（含逻辑名与默认模型名，供前端下拉）。
     */
    public List<AgentModelVO> models() {
        List<AgentModelVO> result = new ArrayList<>();
        Map<String, AgentScopeProperties.ModelProperties> models = properties.getModels();
        if (models != null) {
            for (Map.Entry<String, AgentScopeProperties.ModelProperties> entry : models.entrySet()) {
                AgentScopeProperties.ModelProperties mp = entry.getValue();
                AgentModelVO vo = new AgentModelVO();
                vo.setCode(entry.getKey());
                vo.setType(mp == null ? null : mp.getType());
                vo.setModel(mp == null ? null : mp.getModel());
                result.add(vo);
            }
        }
        return result;
    }

    // ================ 内部方法 ================

    private void apply(AgentDefinitionEntity entity, AgentDefinitionParam param) {
        entity.setName(param.getName());
        entity.setDescription(param.getDescription());
        entity.setSystemPrompt(param.getSystemPrompt());
        entity.setModel(param.getModel());
        entity.setToolNames(toJson(param.getToolNames()));
        entity.setStreaming(param.getStreaming());
        entity.setMaxSteps(param.getMaxSteps());
        entity.setSkills(toJson(param.getSkills()));
        entity.setGroupCode(param.getGroupCode());
        entity.setStatus(param.getStatus());
        entity.setRemark(param.getRemark());
    }

    private void refreshRuntime(AgentDefinitionEntity entity) {
        if (entity.getStatus() == null || entity.getStatus() != STATUS_ENABLED) {
            agentRegistry.remove(entity.getName());
            return;
        }
        AgentDefinition definition = AgentDefinition.builder()
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
        agentRegistry.register(definition);
    }

    private String toJson(List<String> value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalStateException("序列化失败", e);
        }
    }

    private List<String> parseList(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return null;
        }
    }

    private AgentDefinitionVO toVO(AgentDefinitionEntity entity) {
        AgentDefinitionVO vo = new AgentDefinitionVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setDescription(entity.getDescription());
        vo.setSystemPrompt(entity.getSystemPrompt());
        vo.setModel(entity.getModel());
        vo.setToolNames(parseList(entity.getToolNames()));
        vo.setStreaming(entity.getStreaming());
        vo.setMaxSteps(entity.getMaxSteps());
        vo.setSkills(parseList(entity.getSkills()));
        vo.setGroupCode(entity.getGroupCode());
        vo.setSource(entity.getSource());
        vo.setVersion(entity.getVersion());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
