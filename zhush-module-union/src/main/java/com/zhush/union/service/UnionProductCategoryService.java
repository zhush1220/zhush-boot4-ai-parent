package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionProductCategory;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionProductCategoryMapper;
import com.zhush.union.param.UnionProductCategoryParam;
import com.zhush.union.query.UnionProductCategoryQuery;
import com.zhush.union.vo.UnionProductCategoryVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionProductCategoryService
 * @Description 商品分类关联表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionProductCategoryService {

    private final UnionProductCategoryMapper productCategoryMapper;
    private final Converter converter;

    public UnionProductCategoryService(UnionProductCategoryMapper productCategoryMapper, Converter converter) {
        this.productCategoryMapper = productCategoryMapper;
        this.converter = converter;
    }

    /**
     * 新增商品分类关联
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createProductCategory(UnionProductCategoryParam param) {
        UnionProductCategory entity = converter.convert(param, UnionProductCategory.class);
        productCategoryMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改商品分类关联
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProductCategory(UnionProductCategoryParam param) {
        UnionProductCategory existing = productCategoryMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.PRODUCT_CATEGORY_NOT_FOUND, param.getId());
        UnionProductCategory update = converter.convert(param, UnionProductCategory.class);
        productCategoryMapper.update(update);
    }

    /**
     * 删除商品分类关联（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteProductCategory(Long id) {
        UnionProductCategory existing = productCategoryMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.PRODUCT_CATEGORY_NOT_FOUND, id);
        productCategoryMapper.deleteById(id);
    }

    /**
     * 商品分类关联详情
     */
    public UnionProductCategoryVO getProductCategory(Long id) {
        UnionProductCategory entity = productCategoryMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.PRODUCT_CATEGORY_NOT_FOUND, id);
        return converter.convert(entity, UnionProductCategoryVO.class);
    }

    /**
     * 商品分类关联分页
     */
    public PageResult<UnionProductCategoryVO> getProductCategoryPage(UnionProductCategoryQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionProductCategory::getProductId, query.getProductId())
                .eq(UnionProductCategory::getCategoryId, query.getCategoryId())
                .eq(UnionProductCategory::getSortOrder, query.getSortOrder())
        ;
        Page<UnionProductCategory> page = productCategoryMapper.selectPageX(query, queryWrapper);
        List<UnionProductCategoryVO> list = converter.convert(page.getRecords(), UnionProductCategoryVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
