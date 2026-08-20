package com.zhush.union.mapper;


import org.apache.ibatis.annotations.Mapper;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import com.zhush.union.entity.UnionProductStatusLog;

/**
 * @ClassName UnionProductStatusLogMapper
 * @Description 商品状态变更日志表 Mapper
 * @Author zhush
 * @Date 2026/08/07
 **/
@Mapper
public interface UnionProductStatusLogMapper extends BaseMapperX<UnionProductStatusLog> {
}
