package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionAutoAuditConfig;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionAutoAuditConfigMapper;
import com.zhush.union.param.UnionAutoAuditConfigParam;
import com.zhush.union.query.UnionAutoAuditConfigQuery;
import com.zhush.union.vo.UnionAutoAuditConfigVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionAutoAuditConfigService
 * @Description 自动审核配置表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionAutoAuditConfigService {

    private final UnionAutoAuditConfigMapper autoAuditConfigMapper;
    private final Converter converter;

    public UnionAutoAuditConfigService(UnionAutoAuditConfigMapper autoAuditConfigMapper, Converter converter) {
        this.autoAuditConfigMapper = autoAuditConfigMapper;
        this.converter = converter;
    }

    /**
     * 新增自动审核配置
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createAutoAuditConfig(UnionAutoAuditConfigParam param) {
        UnionAutoAuditConfig entity = converter.convert(param, UnionAutoAuditConfig.class);
        autoAuditConfigMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改自动审核配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateAutoAuditConfig(UnionAutoAuditConfigParam param) {
        UnionAutoAuditConfig existing = autoAuditConfigMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.AUTO_AUDIT_CONFIG_NOT_FOUND, param.getId());
        UnionAutoAuditConfig update = converter.convert(param, UnionAutoAuditConfig.class);
        autoAuditConfigMapper.update(update);
    }

    /**
     * 删除自动审核配置（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAutoAuditConfig(Long id) {
        UnionAutoAuditConfig existing = autoAuditConfigMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.AUTO_AUDIT_CONFIG_NOT_FOUND, id);
        autoAuditConfigMapper.deleteById(id);
    }

    /**
     * 自动审核配置详情
     */
    public UnionAutoAuditConfigVO getAutoAuditConfig(Long id) {
        UnionAutoAuditConfig entity = autoAuditConfigMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.AUTO_AUDIT_CONFIG_NOT_FOUND, id);
        return converter.convert(entity, UnionAutoAuditConfigVO.class);
    }

    /**
     * 自动审核配置分页
     */
    public PageResult<UnionAutoAuditConfigVO> getAutoAuditConfigPage(UnionAutoAuditConfigQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(UnionAutoAuditConfig::getConfigKey, query.getConfigKey())
                .like(UnionAutoAuditConfig::getConfigValue, query.getConfigValue())
                .like(UnionAutoAuditConfig::getDescription, query.getDescription())
        ;
        Page<UnionAutoAuditConfig> page = autoAuditConfigMapper.selectPageX(query, queryWrapper);
        List<UnionAutoAuditConfigVO> list = converter.convert(page.getRecords(), UnionAutoAuditConfigVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
