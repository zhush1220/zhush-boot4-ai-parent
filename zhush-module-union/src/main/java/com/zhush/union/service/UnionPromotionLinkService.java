package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionPromotionLink;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionPromotionLinkMapper;
import com.zhush.union.param.UnionPromotionLinkParam;
import com.zhush.union.query.UnionPromotionLinkQuery;
import com.zhush.union.vo.UnionPromotionLinkVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionPromotionLinkService
 * @Description 推广链接表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionPromotionLinkService {

    private final UnionPromotionLinkMapper promotionLinkMapper;
    private final Converter converter;

    public UnionPromotionLinkService(UnionPromotionLinkMapper promotionLinkMapper, Converter converter) {
        this.promotionLinkMapper = promotionLinkMapper;
        this.converter = converter;
    }

    /**
     * 新增推广链接
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createPromotionLink(UnionPromotionLinkParam param) {
        UnionPromotionLink entity = converter.convert(param, UnionPromotionLink.class);
        promotionLinkMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改推广链接
     */
    @Transactional(rollbackFor = Exception.class)
    public void updatePromotionLink(UnionPromotionLinkParam param) {
        UnionPromotionLink existing = promotionLinkMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.PROMOTION_LINK_NOT_FOUND, param.getId());
        UnionPromotionLink update = converter.convert(param, UnionPromotionLink.class);
        promotionLinkMapper.update(update);
    }

    /**
     * 删除推广链接（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deletePromotionLink(Long id) {
        UnionPromotionLink existing = promotionLinkMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.PROMOTION_LINK_NOT_FOUND, id);
        promotionLinkMapper.deleteById(id);
    }

    /**
     * 推广链接详情
     */
    public UnionPromotionLinkVO getPromotionLink(Long id) {
        UnionPromotionLink entity = promotionLinkMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.PROMOTION_LINK_NOT_FOUND, id);
        return converter.convert(entity, UnionPromotionLinkVO.class);
    }

    /**
     * 推广链接分页
     */
    public PageResult<UnionPromotionLinkVO> getPromotionLinkPage(UnionPromotionLinkQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionPromotionLink::getMemberId, query.getMemberId())
                .eq(UnionPromotionLink::getPromoterType, query.getPromoterType())
                .eq(UnionPromotionLink::getExternalPromoterId, query.getExternalPromoterId())
                .eq(UnionPromotionLink::getMasterMemberId, query.getMasterMemberId())
                .eq(UnionPromotionLink::getPlatform, query.getPlatform())
                .like(UnionPromotionLink::getPlatformAdzoneId, query.getPlatformAdzoneId())
                .like(UnionPromotionLink::getPlatformSiteId, query.getPlatformSiteId())
                .eq(UnionPromotionLink::getProductId, query.getProductId())
                .like(UnionPromotionLink::getShopId, query.getShopId())
                .eq(UnionPromotionLink::getLinkType, query.getLinkType())
                .like(UnionPromotionLink::getPromotionUrl, query.getPromotionUrl())
                .like(UnionPromotionLink::getShortUrl, query.getShortUrl())
                .like(UnionPromotionLink::getPromotionCode, query.getPromotionCode())
                .eq(UnionPromotionLink::getClickCount, query.getClickCount())
                .eq(UnionPromotionLink::getOrderCount, query.getOrderCount())
                .eq(UnionPromotionLink::getStatus, query.getStatus())
        ;
        Page<UnionPromotionLink> page = promotionLinkMapper.selectPageX(query, queryWrapper);
        List<UnionPromotionLinkVO> list = converter.convert(page.getRecords(), UnionPromotionLinkVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
