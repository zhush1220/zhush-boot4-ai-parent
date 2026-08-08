package com.zhush.union.service;

import com.mybatisflex.core.paginate.Page;
import com.zhush.common.response.PageResult;
import com.zhush.mybatis.core.query.QueryWrapperX;
import com.zhush.union.entity.UnionCategory;
import com.zhush.union.enums.UnionErrorCode;
import com.zhush.union.mapper.UnionCategoryMapper;
import com.zhush.union.param.UnionCategoryParam;
import com.zhush.union.query.UnionCategoryQuery;
import com.zhush.union.vo.UnionCategoryVO;
import io.github.linpeilie.Converter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zhush.common.exception.ServiceExceptionUtil.assertNotNull;

/**
 * @ClassName UnionCategoryService
 * @Description 商品分类表 服务类
 * @Author zhush
 * @Date 2026/08/08
 **/
@Service
public class UnionCategoryService {

    private final UnionCategoryMapper categoryMapper;
    private final Converter converter;

    public UnionCategoryService(UnionCategoryMapper categoryMapper, Converter converter) {
        this.categoryMapper = categoryMapper;
        this.converter = converter;
    }

    /**
     * 新增商品分类
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createCategory(UnionCategoryParam param) {
        UnionCategory entity = converter.convert(param, UnionCategory.class);
        categoryMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改商品分类
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(UnionCategoryParam param) {
        UnionCategory existing = categoryMapper.selectOneById(param.getId());
        assertNotNull(existing, UnionErrorCode.CATEGORY_NOT_FOUND, param.getId());
        UnionCategory update = converter.convert(param, UnionCategory.class);
        categoryMapper.update(update);
    }

    /**
     * 删除商品分类（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        UnionCategory existing = categoryMapper.selectOneById(id);
        assertNotNull(existing, UnionErrorCode.CATEGORY_NOT_FOUND, id);
        categoryMapper.deleteById(id);
    }

    /**
     * 商品分类详情
     */
    public UnionCategoryVO getCategory(Long id) {
        UnionCategory entity = categoryMapper.selectOneById(id);
        assertNotNull(entity, UnionErrorCode.CATEGORY_NOT_FOUND, id);
        return converter.convert(entity, UnionCategoryVO.class);
    }

    /**
     * 商品分类分页
     */
    public PageResult<UnionCategoryVO> getCategoryPage(UnionCategoryQuery query) {
        QueryWrapperX queryWrapper = QueryWrapperX.create()
                .eq(UnionCategory::getParentId, query.getParentId())
                .like(UnionCategory::getName, query.getName())
                .like(UnionCategory::getIcon, query.getIcon())
                .eq(UnionCategory::getSortOrder, query.getSortOrder())
                .eq(UnionCategory::getStatus, query.getStatus())
        ;
        Page<UnionCategory> page = categoryMapper.selectPageX(query, queryWrapper);
        List<UnionCategoryVO> list = converter.convert(page.getRecords(), UnionCategoryVO.class);
        return PageResult.of(list, page.getTotalRow());
    }

}
