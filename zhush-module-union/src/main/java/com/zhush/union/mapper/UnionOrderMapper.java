package com.zhush.union.mapper;


import org.apache.ibatis.annotations.Mapper;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import com.zhush.union.entity.UnionOrder;

/**
 * @ClassName UnionOrderMapper
 * @Description 联盟订单主表 Mapper
 * @Author zhush
 * @Date 2026/08/07
 **/
@Mapper
public interface UnionOrderMapper extends BaseMapperX<UnionOrder> {
}
