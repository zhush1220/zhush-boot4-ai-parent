package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionPlatformCategoryMap;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionPlatformCategoryMapMapper;
import com.zhush.union.param.UnionPlatformCategoryMapParam;
import com.zhush.union.query.UnionPlatformCategoryMapQuery;
import com.zhush.union.vo.UnionPlatformCategoryMapVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionPlatformCategoryMapService
 * @Description 平台类目映射表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionPlatformCategoryMapService {

    private final UnionPlatformCategoryMapMapper platformCategoryMapMapper;
    private final Converter converter;

    public UnionPlatformCategoryMapService(UnionPlatformCategoryMapMapper platformCategoryMapMapper, Converter converter) {
        this.platformCategoryMapMapper = platformCategoryMapMapper;
        this.converter = converter;
    }

    /**
     * 新增平台类目映射
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createPlatformCategoryMap(UnionPlatformCategoryMapParam param) {
        UnionPlatformCategoryMap entity = converter.convert(param, UnionPlatformCategoryMap.class);
        platformCategoryMapMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改平台类目映射
     */
    @Transactional(rollbackFor = Exception.class)
    public void updatePlatformCategoryMap(UnionPlatformCategoryMapParam param) {
        UnionPlatformCategoryMap existing = platformCategoryMapMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.PLATFORM_CATEGORY_MAP_NOT_FOUND, param.getId());
        UnionPlatformCategoryMap update = converter.convert(param, UnionPlatformCategoryMap.class);
        platformCategoryMapMapper.update(update);
    }

    /**
     * 删除平台类目映射（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deletePlatformCategoryMap(Long id) {
        UnionPlatformCategoryMap existing = platformCategoryMapMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.PLATFORM_CATEGORY_MAP_NOT_FOUND, id);
        platformCategoryMapMapper.deleteById(id);
    }

    /**
     * 平台类目映射详情
     */
    public UnionPlatformCategoryMapVO getPlatformCategoryMap(Long id) {
        UnionPlatformCategoryMap entity = platformCategoryMapMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.PLATFORM_CATEGORY_MAP_NOT_FOUND, id);
        return converter.convert(entity, UnionPlatformCategoryMapVO.class);
    }

    /**
     * 平台类目映射分页
     */
    public PageResult<UnionPlatformCategoryMapVO> getPlatformCategoryMapPage(UnionPlatformCategoryMapQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionPlatformCategoryMap::getPlatform, query.getPlatform())
                .like(UnionPlatformCategoryMap::getPlatformCategoryId, query.getPlatformCategoryId())
                .like(UnionPlatformCategoryMap::getPlatformCategoryName, query.getPlatformCategoryName())
                .like(UnionPlatformCategoryMap::getPlatformParentId, query.getPlatformParentId())
                .eq(UnionPlatformCategoryMap::getUnionCategoryId, query.getUnionCategoryId())
                .eq(UnionPlatformCategoryMap::getStatus, query.getStatus())
        ;
        Page<UnionPlatformCategoryMap> page = platformCategoryMapMapper.selectPageX(query, queryWrapper);
        List<UnionPlatformCategoryMapVO> list = converter.convert(page.getRecords(), UnionPlatformCategoryMapVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
