package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionClickTracking;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionClickTrackingMapper;
import com.zhush.union.param.UnionClickTrackingParam;
import com.zhush.union.query.UnionClickTrackingQuery;
import com.zhush.union.vo.UnionClickTrackingVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionClickTrackingService
 * @Description 推广链接点击明细表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionClickTrackingService {

    private final UnionClickTrackingMapper clickTrackingMapper;
    private final Converter converter;

    public UnionClickTrackingService(UnionClickTrackingMapper clickTrackingMapper, Converter converter) {
        this.clickTrackingMapper = clickTrackingMapper;
        this.converter = converter;
    }

    /**
     * 新增推广链接点击明细
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createClickTracking(UnionClickTrackingParam param) {
        UnionClickTracking entity = converter.convert(param, UnionClickTracking.class);
        clickTrackingMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改推广链接点击明细
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateClickTracking(UnionClickTrackingParam param) {
        UnionClickTracking existing = clickTrackingMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.CLICK_TRACKING_NOT_FOUND, param.getId());
        UnionClickTracking update = converter.convert(param, UnionClickTracking.class);
        clickTrackingMapper.update(update);
    }

    /**
     * 删除推广链接点击明细（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteClickTracking(Long id) {
        UnionClickTracking existing = clickTrackingMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.CLICK_TRACKING_NOT_FOUND, id);
        clickTrackingMapper.deleteById(id);
    }

    /**
     * 推广链接点击明细详情
     */
    public UnionClickTrackingVO getClickTracking(Long id) {
        UnionClickTracking entity = clickTrackingMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.CLICK_TRACKING_NOT_FOUND, id);
        return converter.convert(entity, UnionClickTrackingVO.class);
    }

    /**
     * 推广链接点击明细分页
     */
    public PageResult<UnionClickTrackingVO> getClickTrackingPage(UnionClickTrackingQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(UnionClickTracking::getShortCode, query.getShortCode())
                .eq(UnionClickTracking::getPromotionLinkId, query.getPromotionLinkId())
                .eq(UnionClickTracking::getMemberId, query.getMemberId())
                .eq(UnionClickTracking::getPlatform, query.getPlatform())
                .eq(UnionClickTracking::getProductId, query.getProductId())
                .eq(UnionClickTracking::getUserId, query.getUserId())
                .like(UnionClickTracking::getVisitorId, query.getVisitorId())
                .like(UnionClickTracking::getClickIp, query.getClickIp())
                .like(UnionClickTracking::getUserAgent, query.getUserAgent())
                .like(UnionClickTracking::getReferer, query.getReferer())
                .like(UnionClickTracking::getProvince, query.getProvince())
                .like(UnionClickTracking::getCity, query.getCity())
                .eq(UnionClickTracking::getOrderId, query.getOrderId())
        ;
        Page<UnionClickTracking> page = clickTrackingMapper.selectPageX(query, queryWrapper);
        List<UnionClickTrackingVO> list = converter.convert(page.getRecords(), UnionClickTrackingVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
