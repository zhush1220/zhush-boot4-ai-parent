package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionShortUrlMap;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionShortUrlMapMapper;
import com.zhush.union.param.UnionShortUrlMapParam;
import com.zhush.union.query.UnionShortUrlMapQuery;
import com.zhush.union.vo.UnionShortUrlMapVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionShortUrlMapService
 * @Description 短链接映射表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionShortUrlMapService {

    private final UnionShortUrlMapMapper shortUrlMapMapper;
    private final Converter converter;

    public UnionShortUrlMapService(UnionShortUrlMapMapper shortUrlMapMapper, Converter converter) {
        this.shortUrlMapMapper = shortUrlMapMapper;
        this.converter = converter;
    }

    /**
     * 新增短链接映射
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createShortUrlMap(UnionShortUrlMapParam param) {
        UnionShortUrlMap entity = converter.convert(param, UnionShortUrlMap.class);
        shortUrlMapMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改短链接映射
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateShortUrlMap(UnionShortUrlMapParam param) {
        UnionShortUrlMap existing = shortUrlMapMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.SHORT_URL_MAP_NOT_FOUND, param.getId());
        UnionShortUrlMap update = converter.convert(param, UnionShortUrlMap.class);
        shortUrlMapMapper.update(update);
    }

    /**
     * 删除短链接映射（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteShortUrlMap(Long id) {
        UnionShortUrlMap existing = shortUrlMapMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.SHORT_URL_MAP_NOT_FOUND, id);
        shortUrlMapMapper.deleteById(id);
    }

    /**
     * 短链接映射详情
     */
    public UnionShortUrlMapVO getShortUrlMap(Long id) {
        UnionShortUrlMap entity = shortUrlMapMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.SHORT_URL_MAP_NOT_FOUND, id);
        return converter.convert(entity, UnionShortUrlMapVO.class);
    }

    /**
     * 短链接映射分页
     */
    public PageResult<UnionShortUrlMapVO> getShortUrlMapPage(UnionShortUrlMapQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(UnionShortUrlMap::getShortCode, query.getShortCode())
                .eq(UnionShortUrlMap::getPromotionLinkId, query.getPromotionLinkId())
                .like(UnionShortUrlMap::getOriginalUrl, query.getOriginalUrl())
                .like(UnionShortUrlMap::getTargetUrl, query.getTargetUrl())
                .eq(UnionShortUrlMap::getTotalClicks, query.getTotalClicks())
                .eq(UnionShortUrlMap::getUniqueClicks, query.getUniqueClicks())
                .eq(UnionShortUrlMap::getStatus, query.getStatus())
        ;
        Page<UnionShortUrlMap> page = shortUrlMapMapper.selectPageX(query, queryWrapper);
        List<UnionShortUrlMapVO> list = converter.convert(page.getRecords(), UnionShortUrlMapVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
