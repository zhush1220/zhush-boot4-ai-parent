package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionLinkConfig;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionLinkConfigMapper;
import com.zhush.union.param.UnionLinkConfigParam;
import com.zhush.union.query.UnionLinkConfigQuery;
import com.zhush.union.vo.UnionLinkConfigVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionLinkConfigService
 * @Description 转链配置表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionLinkConfigService {

    private final UnionLinkConfigMapper linkConfigMapper;
    private final Converter converter;

    public UnionLinkConfigService(UnionLinkConfigMapper linkConfigMapper, Converter converter) {
        this.linkConfigMapper = linkConfigMapper;
        this.converter = converter;
    }

    /**
     * 新增转链配置
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createLinkConfig(UnionLinkConfigParam param) {
        UnionLinkConfig entity = converter.convert(param, UnionLinkConfig.class);
        linkConfigMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改转链配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateLinkConfig(UnionLinkConfigParam param) {
        UnionLinkConfig existing = linkConfigMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.LINK_CONFIG_NOT_FOUND, param.getId());
        UnionLinkConfig update = converter.convert(param, UnionLinkConfig.class);
        linkConfigMapper.update(update);
    }

    /**
     * 删除转链配置（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteLinkConfig(Long id) {
        UnionLinkConfig existing = linkConfigMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.LINK_CONFIG_NOT_FOUND, id);
        linkConfigMapper.deleteById(id);
    }

    /**
     * 转链配置详情
     */
    public UnionLinkConfigVO getLinkConfig(Long id) {
        UnionLinkConfig entity = linkConfigMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.LINK_CONFIG_NOT_FOUND, id);
        return converter.convert(entity, UnionLinkConfigVO.class);
    }

    /**
     * 转链配置分页
     */
    public PageResult<UnionLinkConfigVO> getLinkConfigPage(UnionLinkConfigQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(UnionLinkConfig::getConfigName, query.getConfigName())
                .eq(UnionLinkConfig::getMemberLevel, query.getMemberLevel())
                .eq(UnionLinkConfig::getPlatform, query.getPlatform())
                .eq(UnionLinkConfig::getCategoryId, query.getCategoryId())
                .eq(UnionLinkConfig::getShortUrlExpireDays, query.getShortUrlExpireDays())
                .like(UnionLinkConfig::getExpireStrategy, query.getExpireStrategy())
                .eq(UnionLinkConfig::getRateLimitPerMinute, query.getRateLimitPerMinute())
                .eq(UnionLinkConfig::getRateLimitPerHour, query.getRateLimitPerHour())
                .eq(UnionLinkConfig::getPriority, query.getPriority())
                .eq(UnionLinkConfig::getStatus, query.getStatus())
        ;
        Page<UnionLinkConfig> page = linkConfigMapper.selectPageX(query, queryWrapper);
        List<UnionLinkConfigVO> list = converter.convert(page.getRecords(), UnionLinkConfigVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
