package com.zhush.ai.service;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.agentscope.config.AgentScopeProperties;
import com.zhush.agentscope.config.DynamicConfigRefreshCoordinator;
import com.zhush.agentscope.model.DefaultModelFactory;
import com.zhush.ai.entity.AgentDefinitionEntity;
import com.zhush.ai.entity.AiModelEntity;
import com.zhush.ai.mapper.AgentDefinitionMapper;
import com.zhush.ai.mapper.AiModelMapper;
import com.zhush.ai.param.AiModelParam;
import com.zhush.ai.query.AiModelQuery;
import com.zhush.ai.vo.AiModelVO;
import com.zhush.common.exception.ServiceExceptionUtil;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.zhush.ai.enums.AiErrorCode.MODEL_CODE_DUPLICATE;
import static com.zhush.ai.enums.AiErrorCode.MODEL_DEFAULT_CANNOT_DISABLE;
import static com.zhush.ai.enums.AiErrorCode.MODEL_NOT_FOUND;
import static com.zhush.ai.enums.AiErrorCode.MODEL_REFERENCED;
import static com.zhush.ai.enums.AiErrorCode.MODEL_TEST_FAIL;

/**
 * AI 模型配置管理服务：落库 {@code ai_model}，变更后刷新动态模型池并广播多实例。
 *
 * <p>业务规则：编码唯一、默认模型全局互斥、停用/删除前校验启用态智能体引用。
 *
 * @author zhush
 */
@Service
public class AiModelService {

    private static final Logger log = LoggerFactory.getLogger(AiModelService.class);

    private static final int STATUS_ENABLED = 1;
    private static final int STATUS_DISABLED = 0;

    private final AiModelMapper modelMapper;
    private final AgentDefinitionMapper definitionMapper;
    private final AiModelCipher cipher;
    private final ObjectMapper objectMapper;
    private final DefaultModelFactory modelFactory;
    private final DynamicConfigRefreshCoordinator refreshCoordinator;
    private final ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider;

    public AiModelService(AiModelMapper modelMapper, AgentDefinitionMapper definitionMapper,
                          AiModelCipher cipher, ObjectMapper objectMapper,
                          DefaultModelFactory modelFactory,
                          DynamicConfigRefreshCoordinator refreshCoordinator,
                          ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider) {
        this.modelMapper = modelMapper;
        this.definitionMapper = definitionMapper;
        this.cipher = cipher;
        this.objectMapper = objectMapper;
        this.modelFactory = modelFactory;
        this.refreshCoordinator = refreshCoordinator;
        this.redisTemplateProvider = redisTemplateProvider;
    }

    /**
     * 分页查询。
     */
    public PageResult<AiModelVO> page(AiModelQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(AiModelEntity::getCode, query.getCode())
                .eq(AiModelEntity::getType, query.getType())
                .eq(AiModelEntity::getCapability, query.getCapability())
                .eq(AiModelEntity::getStatus, query.getStatus());
        queryWrapper.orderBy("priority", false).orderBy("create_time", false);
        Page<AiModelEntity> page = modelMapper.selectPageX(query, queryWrapper);
        List<AiModelVO> list = new ArrayList<>(page.getRecords().size());
        for (AiModelEntity entity : page.getRecords()) {
            list.add(toVO(entity));
        }
        return PageResult.of(list, page.getTotalRow());
    }

    /**
     * 详情（apiKey 掩码）。
     */
    public AiModelVO get(Long id) {
        AiModelEntity entity = modelMapper.selectOneById(id);
        ServiceExceptionUtil.assertNotNull(entity, MODEL_NOT_FOUND, id);
        return toVO(entity);
    }

    /**
     * 启用态模型列表（Agent 表单下拉，按优先级倒序）。
     */
    public List<AiModelVO> listEnabled() {
        List<AiModelEntity> entities = modelMapper.selectEnabled();
        List<AiModelVO> result = new ArrayList<>(entities.size());
        for (AiModelEntity entity : entities) {
            result.add(toVO(entity));
        }
        return result;
    }

    /**
     * 新增（编码唯一校验 + apiKey 加密 + 默认互斥）。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(AiModelParam param) {
        if (modelMapper.selectByCode(param.getCode()) != null) {
            throw ServiceExceptionUtil.exception(MODEL_CODE_DUPLICATE, param.getCode());
        }
        AiModelEntity entity = new AiModelEntity();
        apply(entity, param, true);
        if (entity.getStatus() == null) {
            entity.setStatus(STATUS_ENABLED);
        }
        modelMapper.insert(entity);
        refreshAndBroadcast();
        return entity.getId();
    }

    /**
     * 编辑（编码不可变更，apiKey 为空保留原值）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(AiModelParam param) {
        AiModelEntity entity = modelMapper.selectOneById(param.getId());
        ServiceExceptionUtil.assertNotNull(entity, MODEL_NOT_FOUND, param.getId());
        apply(entity, param, false);
        modelMapper.update(entity);
        refreshAndBroadcast();
    }

    /**
     * 删除（被引用阻断）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AiModelEntity entity = modelMapper.selectOneById(id);
        if (entity == null) {
            return;
        }
        checkNotReferenced(entity.getCode());
        modelMapper.deleteById(id);
        refreshAndBroadcast();
    }

    /**
     * 批量删除（循环复用单条删除的校验与动态池刷新）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        ids.forEach(this::delete);
    }

    /**
     * 启停（停用校验引用与默认模型）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void toggle(Long id) {
        AiModelEntity entity = modelMapper.selectOneById(id);
        ServiceExceptionUtil.assertNotNull(entity, MODEL_NOT_FOUND, id);
        if (entity.getStatus() != null && entity.getStatus() == STATUS_ENABLED) {
            // 停用
            if (entity.getIsDefault() != null && entity.getIsDefault() == 1) {
                throw ServiceExceptionUtil.exception(MODEL_DEFAULT_CANNOT_DISABLE);
            }
            checkNotReferenced(entity.getCode());
            entity.setStatus(STATUS_DISABLED);
        } else {
            entity.setStatus(STATUS_ENABLED);
        }
        modelMapper.update(entity);
        refreshAndBroadcast();
    }

    /**
     * 连接测试：以该模型配置发起一次最小调用。
     */
    public void test(Long id) {
        AiModelEntity entity = modelMapper.selectOneById(id);
        ServiceExceptionUtil.assertNotNull(entity, MODEL_NOT_FOUND, id);
        AgentScopeProperties.ModelProperties mp = toModelProperties(entity);
        try {
            Model model = modelFactory.create(mp);
            Msg userMsg = Msg.builder().role(MsgRole.USER).textContent("ping").build();
            model.stream(List.of(userMsg), List.of(), null).blockLast(Duration.ofSeconds(20));
        } catch (Exception e) {
            log.warn("模型连接测试失败, code={}", entity.getCode(), e);
            throw ServiceExceptionUtil.exception(MODEL_TEST_FAIL, e.getMessage());
        }
    }

    /**
     * 启用态模型实体（供 {@link com.zhush.ai.provider.DbModelConfigProvider} 输出动态配置）。
     */
    public List<AiModelEntity> enabledEntities() {
        return modelMapper.selectEnabled();
    }

    /**
     * 解密并解析 apiKey 明文（仅运行态使用，禁止外传）。
     */
    public String resolveApiKey(AiModelEntity entity) {
        return cipher.resolve(entity.getApiKey());
    }

    // ================ 内部方法 ================

    private void apply(AiModelEntity entity, AiModelParam param, boolean isCreate) {
        if (isCreate) {
            entity.setCode(param.getCode());
        }
        entity.setName(param.getName());
        entity.setType(param.getType());
        entity.setBaseUrl(param.getBaseUrl() == null ? "" : param.getBaseUrl());
        entity.setModel(param.getModel());
        entity.setCapability(param.getCapability() == null || param.getCapability().isBlank()
                ? "text" : param.getCapability());
        // 单价未传时保留原值（列 NOT NULL DEFAULT 0，新增时由默认值补 0），避免误清空已配价格
        if (param.getInputPrice() != null) {
            entity.setInputPrice(param.getInputPrice());
        }
        if (param.getOutputPrice() != null) {
            entity.setOutputPrice(param.getOutputPrice());
        }
        entity.setPriority(param.getPriority() == null ? 0 : param.getPriority());
        entity.setOptions(toOptionsJson(param.getOptions()));
        entity.setRemark(param.getRemark());
        // apiKey：编辑时空值或掩码保留原值
        if (isCreate || (param.getApiKey() != null && !param.getApiKey().isBlank())) {
            entity.setApiKey(cipher.encrypt(param.getApiKey() == null ? "" : param.getApiKey()));
        }
        if (param.getStatus() != null) {
            entity.setStatus(param.getStatus());
        }
        // 默认模型互斥：设新默认时清除旧默认
        Integer isDefault = param.getIsDefault() == null ? 0 : param.getIsDefault();
        if (isDefault == 1) {
            AiModelEntity oldDefault = modelMapper.selectDefault();
            if (oldDefault != null && !oldDefault.getId().equals(entity.getId())) {
                oldDefault.setIsDefault(0);
                modelMapper.update(oldDefault);
            }
        }
        entity.setIsDefault(isDefault);
    }

    private void checkNotReferenced(String code) {
        List<String> names = referencedAgentNames(code);
        if (!names.isEmpty()) {
            throw ServiceExceptionUtil.exception(MODEL_REFERENCED, code + "（引用方：" + String.join("、", names) + "）");
        }
    }

    private List<String> referencedAgentNames(String code) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(AgentDefinitionEntity::getModel).eq(code);
        queryWrapper.where(AgentDefinitionEntity::getStatus).eq(1);
        List<AgentDefinitionEntity> agents = definitionMapper.selectListByQuery(queryWrapper);
        List<String> names = new ArrayList<>(agents.size());
        for (AgentDefinitionEntity agent : agents) {
            names.add(agent.getName());
        }
        return names;
    }

    /**
     * 本实例刷新 + Redis pub/sub 广播（多实例同步）。
     */
    private void refreshAndBroadcast() {
        refreshCoordinator.refreshAll();
        RedisTemplate<String, Object> redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate != null) {
            try {
                redisTemplate.convertAndSend(DynamicConfigRefreshCoordinator.REFRESH_CHANNEL, "refresh");
            } catch (Exception e) {
                log.warn("动态配置刷新广播失败（不影响本实例生效）", e);
            }
        }
    }

    private AgentScopeProperties.ModelProperties toModelProperties(AiModelEntity entity) {
        AgentScopeProperties.ModelProperties mp = new AgentScopeProperties.ModelProperties();
        mp.setType(entity.getType() == null || entity.getType().isEmpty() ? "dashscope" : entity.getType());
        mp.setApiKey(cipher.resolve(entity.getApiKey()));
        mp.setBaseUrl(entity.getBaseUrl());
        mp.setModel(entity.getModel());
        Map<String, Object> options = parseOptions(entity.getOptions());
        if (options != null) {
            mp.setOptions(options);
        }
        return mp;
    }

    private String toOptionsJson(Map<String, Object> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(options);
        } catch (JacksonException e) {
            throw new IllegalStateException("序列化模型透传参数失败", e);
        }
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

    private AiModelVO toVO(AiModelEntity entity) {
        AiModelVO vo = new AiModelVO();
        vo.setId(entity.getId());
        vo.setCode(entity.getCode());
        vo.setName(entity.getName());
        vo.setType(entity.getType());
        vo.setBaseUrl(entity.getBaseUrl());
        vo.setModel(entity.getModel());
        vo.setCapability(entity.getCapability());
        vo.setInputPrice(entity.getInputPrice());
        vo.setOutputPrice(entity.getOutputPrice());
        vo.setIsDefault(entity.getIsDefault());
        vo.setPriority(entity.getPriority());
        vo.setApiKeyMasked(cipher.mask(cipher.resolve(entity.getApiKey())));
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
