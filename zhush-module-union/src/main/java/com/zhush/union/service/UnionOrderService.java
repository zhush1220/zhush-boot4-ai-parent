package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionOrder;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionOrderMapper;
import com.zhush.union.param.UnionOrderParam;
import com.zhush.union.query.UnionOrderQuery;
import com.zhush.union.vo.UnionOrderVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionOrderService
 * @Description 联盟订单主表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionOrderService {

    private final UnionOrderMapper orderMapper;
    private final Converter converter;

    public UnionOrderService(UnionOrderMapper orderMapper, Converter converter) {
        this.orderMapper = orderMapper;
        this.converter = converter;
    }

    /**
     * 新增联盟订单主
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(UnionOrderParam param) {
        UnionOrder entity = converter.convert(param, UnionOrder.class);
        orderMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改联盟订单主
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(UnionOrderParam param) {
        UnionOrder existing = orderMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.ORDER_NOT_FOUND, param.getId());
        UnionOrder update = converter.convert(param, UnionOrder.class);
        orderMapper.update(update);
    }

    /**
     * 删除联盟订单主（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long id) {
        UnionOrder existing = orderMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.ORDER_NOT_FOUND, id);
        orderMapper.deleteById(id);
    }

    /**
     * 联盟订单主详情
     */
    public UnionOrderVO getOrder(Long id) {
        UnionOrder entity = orderMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.ORDER_NOT_FOUND, id);
        return converter.convert(entity, UnionOrderVO.class);
    }

    /**
     * 联盟订单主分页
     */
    public PageResult<UnionOrderVO> getOrderPage(UnionOrderQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .like(UnionOrder::getShardingKey, query.getShardingKey())
                .eq(UnionOrder::getPlatform, query.getPlatform())
                .eq(UnionOrder::getOrderSource, query.getOrderSource())
                .like(UnionOrder::getOuterOrderId, query.getOuterOrderId())
                .like(UnionOrder::getParentOrderId, query.getParentOrderId())
                .eq(UnionOrder::getMemberId, query.getMemberId())
                .eq(UnionOrder::getPromoterType, query.getPromoterType())
                .eq(UnionOrder::getExternalPromoterId, query.getExternalPromoterId())
                .eq(UnionOrder::getMasterMemberId, query.getMasterMemberId())
                .eq(UnionOrder::getPromotionLinkId, query.getPromotionLinkId())
                .eq(UnionOrder::getProductId, query.getProductId())
                .like(UnionOrder::getProductTitle, query.getProductTitle())
                .like(UnionOrder::getProductImage, query.getProductImage())
                .like(UnionOrder::getOuterProductId, query.getOuterProductId())
                .like(UnionOrder::getShopName, query.getShopName())
                .eq(UnionOrder::getOrderStatus, query.getOrderStatus())
                .like(UnionOrder::getPlatformOrderStatus, query.getPlatformOrderStatus())
                .eq(UnionOrder::getSettleStatus, query.getSettleStatus())
                .like(UnionOrder::getExtraJson, query.getExtraJson())
        ;
        Page<UnionOrder> page = orderMapper.selectPageX(query, queryWrapper);
        List<UnionOrderVO> list = converter.convert(page.getRecords(), UnionOrderVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
