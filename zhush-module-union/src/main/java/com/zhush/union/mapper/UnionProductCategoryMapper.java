package com.zhush.union.mapper;


import org.apache.ibatis.annotations.Mapper;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import com.zhush.union.entity.UnionProductCategory;

/**
 * @ClassName UnionProductCategoryMapper
 * @Description 商品分类关联表 Mapper
 * @Author zhush
 * @Date 2026/08/07
 **/
@Mapper
public interface UnionProductCategoryMapper extends BaseMapperX<UnionProductCategory> {
}
