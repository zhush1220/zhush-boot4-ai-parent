package com.zhush.ai.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.ai.entity.AiQuotaRuleEntity;
import com.zhush.ai.enums.AiTaskType;
import com.zhush.ai.mapper.AiQuotaRuleMapper;
import com.zhush.ai.param.AiQuotaRuleParam;
import com.zhush.ai.query.AiQuotaRuleQuery;
import com.zhush.ai.vo.AiQuotaRuleVO;
import com.zhush.common.exception.ServiceExceptionUtil;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.zhush.ai.enums.AiErrorCode.QUOTA_RULE_CONFIG_INVALID;
import static com.zhush.ai.enums.AiErrorCode.QUOTA_RULE_DUPLICATE;
import static com.zhush.ai.enums.AiErrorCode.QUOTA_RULE_NOT_FOUND;

/**
 * AI 配额规则管理服务：落库 {@code ai_quota_rule}，供 {@link AiQuotaService} 运行时判定读取。
 *
 * <p>规则实时生效（判定侧每次直查 DB，无缓存），故本服务变更后无需广播刷新。
 * 唯一口径：{@code user_type + task_type} 一条规则（与表 uk 对齐），新增/编辑做重复校验。
 *
 * @author zhush
 */
@Service
public class AiQuotaRuleService {

    /** 允许配置配额的用户类型白名单（与登录体系 StpAdminUtil/StpMemberUtil 对齐） */
    private static final Set<String> ALLOWED_USER_TYPES = Set.of("admin", "member");

    private final AiQuotaRuleMapper quotaRuleMapper;

    public AiQuotaRuleService(AiQuotaRuleMapper quotaRuleMapper) {
        this.quotaRuleMapper = quotaRuleMapper;
    }

    /**
     * 分页查询。
     */
    public PageResult<AiQuotaRuleVO> page(AiQuotaRuleQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(AiQuotaRuleEntity::getUserType, query.getUserType())
                .eq(AiQuotaRuleEntity::getTaskType, query.getTaskType())
                .eq(AiQuotaRuleEntity::getStatus, query.getStatus());
        queryWrapper.orderBy("id", true);
        Page<AiQuotaRuleEntity> page = quotaRuleMapper.selectPageX(query, queryWrapper);
        List<AiQuotaRuleVO> list = new ArrayList<>(page.getRecords().size());
        for (AiQuotaRuleEntity entity : page.getRecords()) {
            list.add(toVO(entity));
        }
        return PageResult.of(list, page.getTotalRow());
    }

    /**
     * 详情。
     */
    public AiQuotaRuleVO get(Long id) {
        AiQuotaRuleEntity entity = quotaRuleMapper.selectOneById(id);
        ServiceExceptionUtil.assertNotNull(entity, QUOTA_RULE_NOT_FOUND, id);
        return toVO(entity);
    }

    /**
     * 新增（userType + taskType 唯一校验）。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(AiQuotaRuleParam param) {
        normalizeAndValidate(param);
        assertNotDuplicate(param.getUserType(), param.getTaskType(), null);
        AiQuotaRuleEntity entity = new AiQuotaRuleEntity();
        apply(entity, param);
        quotaRuleMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 编辑（组合唯一校验，排除自身）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(AiQuotaRuleParam param) {
        AiQuotaRuleEntity entity = quotaRuleMapper.selectOneById(param.getId());
        ServiceExceptionUtil.assertNotNull(entity, QUOTA_RULE_NOT_FOUND, param.getId());
        normalizeAndValidate(param);
        assertNotDuplicate(param.getUserType(), param.getTaskType(), param.getId());
        apply(entity, param);
        quotaRuleMapper.update(entity);
    }

    /**
     * 删除。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (quotaRuleMapper.selectOneById(id) == null) {
            return;
        }
        quotaRuleMapper.deleteById(id);
    }

    /**
     * 启停（停用即不校验该规则）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void toggle(Long id) {
        AiQuotaRuleEntity entity = quotaRuleMapper.selectOneById(id);
        ServiceExceptionUtil.assertNotNull(entity, QUOTA_RULE_NOT_FOUND, id);
        entity.setStatus(entity.getStatus() != null
                && entity.getStatus() == AiQuotaRuleEntity.STATUS_ENABLED
                ? AiQuotaRuleEntity.STATUS_DISABLED : AiQuotaRuleEntity.STATUS_ENABLED);
        quotaRuleMapper.update(entity);
    }

    // ================ 内部方法 ================

    /**
     * 入参规整与业务校验：userType 白名单、taskType 必须命中枚举（不回落 chat）、上限非负。
     */
    private void normalizeAndValidate(AiQuotaRuleParam param) {
        param.setUserType(param.getUserType().trim().toLowerCase());
        param.setTaskType(param.getTaskType().trim().toLowerCase());
        if (!ALLOWED_USER_TYPES.contains(param.getUserType())) {
            throw ServiceExceptionUtil.exception(QUOTA_RULE_CONFIG_INVALID, "用户类型仅支持 admin/member");
        }
        boolean knownTaskType = false;
        for (AiTaskType type : AiTaskType.values()) {
            if (type.getCode().equals(param.getTaskType())) {
                knownTaskType = true;
                break;
            }
        }
        if (!knownTaskType) {
            throw ServiceExceptionUtil.exception(QUOTA_RULE_CONFIG_INVALID, "未知任务类型：" + param.getTaskType());
        }
        if (negative(param.getDailyLimit()) || negative(param.getMonthlyLimit())) {
            throw ServiceExceptionUtil.exception(QUOTA_RULE_CONFIG_INVALID, "次数上限不能为负数");
        }
    }

    private boolean negative(Integer value) {
        return value != null && value < 0;
    }

    private void assertNotDuplicate(String userType, String taskType, Long excludeId) {
        AiQuotaRuleEntity existing = quotaRuleMapper.selectOneByQuery(QueryWrapper.create()
                .where(AiQuotaRuleEntity::getUserType).eq(userType)
                .and(AiQuotaRuleEntity::getTaskType).eq(taskType));
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw ServiceExceptionUtil.exception(QUOTA_RULE_DUPLICATE, userType + "/" + taskType);
        }
    }

    private void apply(AiQuotaRuleEntity entity, AiQuotaRuleParam param) {
        entity.setUserType(param.getUserType());
        entity.setTaskType(param.getTaskType());
        entity.setDailyLimit(param.getDailyLimit() == null ? 0 : param.getDailyLimit());
        entity.setMonthlyLimit(param.getMonthlyLimit() == null ? 0 : param.getMonthlyLimit());
        entity.setStatus(param.getStatus() == null ? AiQuotaRuleEntity.STATUS_ENABLED : param.getStatus());
        entity.setRemark(param.getRemark());
    }

    private AiQuotaRuleVO toVO(AiQuotaRuleEntity entity) {
        AiQuotaRuleVO vo = new AiQuotaRuleVO();
        vo.setId(entity.getId());
        vo.setUserType(entity.getUserType());
        vo.setTaskType(entity.getTaskType());
        vo.setTaskTypeName(AiTaskType.of(entity.getTaskType()).getDesc());
        vo.setDailyLimit(entity.getDailyLimit());
        vo.setMonthlyLimit(entity.getMonthlyLimit());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
