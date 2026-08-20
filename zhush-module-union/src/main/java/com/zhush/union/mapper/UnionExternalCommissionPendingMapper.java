package com.zhush.union.mapper;


import org.apache.ibatis.annotations.Mapper;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import com.zhush.union.entity.UnionExternalCommissionPending;

/**
 * @ClassName UnionExternalCommissionPendingMapper
 * @Description 外部推广者待结算佣金表 Mapper
 * @Author zhush
 * @Date 2026/08/07
 **/
@Mapper
public interface UnionExternalCommissionPendingMapper extends BaseMapperX<UnionExternalCommissionPending> {
}
