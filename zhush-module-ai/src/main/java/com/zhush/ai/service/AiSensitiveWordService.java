package com.zhush.ai.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.agentscope.config.DynamicConfigRefreshCoordinator;
import com.zhush.ai.entity.AiSensitiveWordEntity;
import com.zhush.ai.mapper.AiSensitiveWordMapper;
import com.zhush.ai.param.AiSensitiveWordParam;
import com.zhush.ai.query.AiSensitiveWordQuery;
import com.zhush.ai.vo.AiSensitiveWordVO;
import com.zhush.common.exception.ServiceExceptionUtil;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.zhush.ai.enums.AiErrorCode.SENSITIVE_WORD_DUPLICATE;
import static com.zhush.ai.enums.AiErrorCode.SENSITIVE_WORD_NOT_FOUND;

/**
 * AI 敏感词库管理服务：落库 {@code ai_sensitive_word}，变更后刷新审批门禁词库并广播多实例。
 *
 * @author zhush
 */
@Service
public class AiSensitiveWordService {

    private static final Logger log = LoggerFactory.getLogger(AiSensitiveWordService.class);

    private static final int STATUS_ENABLED = 1;
    private static final int STATUS_DISABLED = 0;

    private final AiSensitiveWordMapper wordMapper;
    private final DynamicConfigRefreshCoordinator refreshCoordinator;
    private final ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider;

    public AiSensitiveWordService(AiSensitiveWordMapper wordMapper,
                                  DynamicConfigRefreshCoordinator refreshCoordinator,
                                  ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider) {
        this.wordMapper = wordMapper;
        this.refreshCoordinator = refreshCoordinator;
        this.redisTemplateProvider = redisTemplateProvider;
    }

    /**
     * 分页查询。
     */
    public PageResult<AiSensitiveWordVO> page(AiSensitiveWordQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(AiSensitiveWordEntity::getWord, query.getWord())
                .eq(AiSensitiveWordEntity::getLevel, query.getLevel())
                .eq(AiSensitiveWordEntity::getStatus, query.getStatus())
                .eq(AiSensitiveWordEntity::getGroupCode, query.getGroupCode());
        queryWrapper.orderBy("create_time", false);
        Page<AiSensitiveWordEntity> page = wordMapper.selectPageX(query, queryWrapper);
        List<AiSensitiveWordVO> list = new ArrayList<>(page.getRecords().size());
        for (AiSensitiveWordEntity entity : page.getRecords()) {
            list.add(toVO(entity));
        }
        return PageResult.of(list, page.getTotalRow());
    }

    /**
     * 详情。
     */
    public AiSensitiveWordVO get(Long id) {
        AiSensitiveWordEntity entity = wordMapper.selectOneById(id);
        ServiceExceptionUtil.assertNotNull(entity, SENSITIVE_WORD_NOT_FOUND, id);
        return toVO(entity);
    }

    /**
     * 新增（词唯一校验）。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(AiSensitiveWordParam param) {
        String word = param.getWord().trim();
        if (wordMapper.selectByWord(word) != null) {
            throw ServiceExceptionUtil.exception(SENSITIVE_WORD_DUPLICATE, word);
        }
        AiSensitiveWordEntity entity = new AiSensitiveWordEntity();
        apply(entity, param);
        entity.setWord(word);
        if (entity.getStatus() == null) {
            entity.setStatus(STATUS_ENABLED);
        }
        wordMapper.insert(entity);
        refreshAndBroadcast();
        return entity.getId();
    }

    /**
     * 编辑（词变更时唯一校验）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(AiSensitiveWordParam param) {
        AiSensitiveWordEntity entity = wordMapper.selectOneById(param.getId());
        ServiceExceptionUtil.assertNotNull(entity, SENSITIVE_WORD_NOT_FOUND, param.getId());
        String word = param.getWord().trim();
        AiSensitiveWordEntity existing = wordMapper.selectByWord(word);
        if (existing != null && !existing.getId().equals(entity.getId())) {
            throw ServiceExceptionUtil.exception(SENSITIVE_WORD_DUPLICATE, word);
        }
        apply(entity, param);
        entity.setWord(word);
        wordMapper.update(entity);
        refreshAndBroadcast();
    }

    /**
     * 删除。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AiSensitiveWordEntity entity = wordMapper.selectOneById(id);
        if (entity == null) {
            return;
        }
        wordMapper.deleteById(id);
        refreshAndBroadcast();
    }

    /**
     * 启停。
     */
    @Transactional(rollbackFor = Exception.class)
    public void toggle(Long id) {
        AiSensitiveWordEntity entity = wordMapper.selectOneById(id);
        ServiceExceptionUtil.assertNotNull(entity, SENSITIVE_WORD_NOT_FOUND, id);
        entity.setStatus(entity.getStatus() != null && entity.getStatus() == STATUS_ENABLED
                ? STATUS_DISABLED : STATUS_ENABLED);
        wordMapper.update(entity);
        refreshAndBroadcast();
    }

    /**
     * 启用态敏感词实体（供 {@link com.zhush.ai.provider.DbSensitiveWordProvider} 输出动态配置）。
     */
    public List<AiSensitiveWordEntity> enabledEntities() {
        return wordMapper.selectEnabled();
    }

    // ================ 内部方法 ================

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

    private void apply(AiSensitiveWordEntity entity, AiSensitiveWordParam param) {
        entity.setMatchType(param.getMatchType() == null || param.getMatchType().isBlank()
                ? "tool_name" : param.getMatchType());
        entity.setLevel(param.getLevel());
        entity.setGroupCode(param.getGroupCode() == null || param.getGroupCode().isBlank()
                ? "common" : param.getGroupCode());
        entity.setStatus(param.getStatus());
        entity.setRemark(param.getRemark());
    }

    private AiSensitiveWordVO toVO(AiSensitiveWordEntity entity) {
        AiSensitiveWordVO vo = new AiSensitiveWordVO();
        vo.setId(entity.getId());
        vo.setWord(entity.getWord());
        vo.setMatchType(entity.getMatchType());
        vo.setLevel(entity.getLevel());
        vo.setGroupCode(entity.getGroupCode());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
