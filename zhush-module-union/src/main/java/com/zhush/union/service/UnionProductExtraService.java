package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionProductExtra;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionProductExtraMapper;
import com.zhush.union.param.UnionProductExtraParam;
import com.zhush.union.query.UnionProductExtraQuery;
import com.zhush.union.vo.UnionProductExtraVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionProductExtraService
 * @Description 商品扩展信息表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionProductExtraService {

    private final UnionProductExtraMapper productExtraMapper;
    private final Converter converter;

    public UnionProductExtraService(UnionProductExtraMapper productExtraMapper, Converter converter) {
        this.productExtraMapper = productExtraMapper;
        this.converter = converter;
    }

    /**
     * 新增商品扩展信息
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createProductExtra(UnionProductExtraParam param) {
        UnionProductExtra entity = converter.convert(param, UnionProductExtra.class);
        productExtraMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改商品扩展信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProductExtra(UnionProductExtraParam param) {
        UnionProductExtra existing = productExtraMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.PRODUCT_EXTRA_NOT_FOUND, param.getId());
        UnionProductExtra update = converter.convert(param, UnionProductExtra.class);
        productExtraMapper.update(update);
    }

    /**
     * 删除商品扩展信息（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteProductExtra(Long id) {
        UnionProductExtra existing = productExtraMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.PRODUCT_EXTRA_NOT_FOUND, id);
        productExtraMapper.deleteById(id);
    }

    /**
     * 商品扩展信息详情
     */
    public UnionProductExtraVO getProductExtra(Long id) {
        UnionProductExtra entity = productExtraMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.PRODUCT_EXTRA_NOT_FOUND, id);
        return converter.convert(entity, UnionProductExtraVO.class);
    }

    /**
     * 商品扩展信息分页
     */
    public PageResult<UnionProductExtraVO> getProductExtraPage(UnionProductExtraQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionProductExtra::getProductId, query.getProductId())
                .like(UnionProductExtra::getDetailUrl, query.getDetailUrl())
                .like(UnionProductExtra::getDesc, query.getDesc())
                .like(UnionProductExtra::getBrandName, query.getBrandName())
                .like(UnionProductExtra::getBrandId, query.getBrandId())
                .like(UnionProductExtra::getProvince, query.getProvince())
                .like(UnionProductExtra::getAfterSaleService, query.getAfterSaleService())
                .like(UnionProductExtra::getExtraJson, query.getExtraJson())
        ;
        Page<UnionProductExtra> page = productExtraMapper.selectPageX(query, queryWrapper);
        List<UnionProductExtraVO> list = converter.convert(page.getRecords(), UnionProductExtraVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
