package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionLevelConfig;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionLevelConfigMapper;
import com.zhush.union.param.UnionLevelConfigParam;
import com.zhush.union.query.UnionLevelConfigQuery;
import com.zhush.union.vo.UnionLevelConfigVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionLevelConfigService
 * @Description 层级佣金配置表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionLevelConfigService {

    private final UnionLevelConfigMapper levelConfigMapper;
    private final Converter converter;

    public UnionLevelConfigService(UnionLevelConfigMapper levelConfigMapper, Converter converter) {
        this.levelConfigMapper = levelConfigMapper;
        this.converter = converter;
    }

    /**
     * 新增层级佣金配置
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createLevelConfig(UnionLevelConfigParam param) {
        UnionLevelConfig entity = converter.convert(param, UnionLevelConfig.class);
        levelConfigMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改层级佣金配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateLevelConfig(UnionLevelConfigParam param) {
        UnionLevelConfig existing = levelConfigMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.LEVEL_CONFIG_NOT_FOUND, param.getId());
        UnionLevelConfig update = converter.convert(param, UnionLevelConfig.class);
        levelConfigMapper.update(update);
    }

    /**
     * 删除层级佣金配置（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteLevelConfig(Long id) {
        UnionLevelConfig existing = levelConfigMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.LEVEL_CONFIG_NOT_FOUND, id);
        levelConfigMapper.deleteById(id);
    }

    /**
     * 层级佣金配置详情
     */
    public UnionLevelConfigVO getLevelConfig(Long id) {
        UnionLevelConfig entity = levelConfigMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.LEVEL_CONFIG_NOT_FOUND, id);
        return converter.convert(entity, UnionLevelConfigVO.class);
    }

    /**
     * 层级佣金配置分页
     */
    public PageResult<UnionLevelConfigVO> getLevelConfigPage(UnionLevelConfigQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionLevelConfig::getLevel, query.getLevel())
                .like(UnionLevelConfig::getLevelName, query.getLevelName())
                .eq(UnionLevelConfig::getMinTeamSize, query.getMinTeamSize())
                .eq(UnionLevelConfig::getSortOrder, query.getSortOrder())
        ;
        Page<UnionLevelConfig> page = levelConfigMapper.selectPageX(query, queryWrapper);
        List<UnionLevelConfigVO> list = converter.convert(page.getRecords(), UnionLevelConfigVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
