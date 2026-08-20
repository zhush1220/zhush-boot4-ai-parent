package com.zhush.union.mapper;


import org.apache.ibatis.annotations.Mapper;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import com.zhush.union.entity.UnionApiCallLog;

/**
 * @ClassName UnionApiCallLogMapper
 * @Description 平台API调用日志表 Mapper
 * @Author zhush
 * @Date 2026/08/07
 **/
@Mapper
public interface UnionApiCallLogMapper extends BaseMapperX<UnionApiCallLog> {
}
