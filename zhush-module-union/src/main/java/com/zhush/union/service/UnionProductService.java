package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionProduct;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionProductMapper;
import com.zhush.union.param.UnionProductParam;
import com.zhush.union.query.UnionProductQuery;
import com.zhush.union.vo.UnionProductVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionProductService
 * @Description 联盟商品主表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionProductService {

    private final UnionProductMapper productMapper;
    private final Converter converter;

    public UnionProductService(UnionProductMapper productMapper, Converter converter) {
        this.productMapper = productMapper;
        this.converter = converter;
    }

    /**
     * 新增联盟商品主
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createProduct(UnionProductParam param) {
        UnionProduct entity = converter.convert(param, UnionProduct.class);
        productMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改联盟商品主
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(UnionProductParam param) {
        UnionProduct existing = productMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.PRODUCT_NOT_FOUND, param.getId());
        UnionProduct update = converter.convert(param, UnionProduct.class);
        productMapper.update(update);
    }

    /**
     * 删除联盟商品主（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id) {
        UnionProduct existing = productMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.PRODUCT_NOT_FOUND, id);
        productMapper.deleteById(id);
    }

    /**
     * 联盟商品主详情
     */
    public UnionProductVO getProduct(Long id) {
        UnionProduct entity = productMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.PRODUCT_NOT_FOUND, id);
        return converter.convert(entity, UnionProductVO.class);
    }

    /**
     * 联盟商品主分页
     */
    public PageResult<UnionProductVO> getProductPage(UnionProductQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionProduct::getPlatform, query.getPlatform())
                .like(UnionProduct::getOuterProductId, query.getOuterProductId())
                .like(UnionProduct::getTitle, query.getTitle())
                .like(UnionProduct::getSubTitle, query.getSubTitle())
                .like(UnionProduct::getImageUrl, query.getImageUrl())
                .like(UnionProduct::getImageList, query.getImageList())
                .like(UnionProduct::getShopName, query.getShopName())
                .like(UnionProduct::getShopId, query.getShopId())
                .like(UnionProduct::getCategoryId, query.getCategoryId())
                .like(UnionProduct::getCategoryName, query.getCategoryName())
                .eq(UnionProduct::getSalesVolume, query.getSalesVolume())
                .eq(UnionProduct::getGoodComments, query.getGoodComments())
                .like(UnionProduct::getCouponId, query.getCouponId())
                .eq(UnionProduct::getStatus, query.getStatus())
                .eq(UnionProduct::getSyncStatus, query.getSyncStatus())
        ;
        Page<UnionProduct> page = productMapper.selectPageX(query, queryWrapper);
        List<UnionProductVO> list = converter.convert(page.getRecords(), UnionProductVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
