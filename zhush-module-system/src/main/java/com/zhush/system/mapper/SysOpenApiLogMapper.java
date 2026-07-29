package com.zhush.system.mapper;

import com.zhush.mybatis.core.mapper.BaseMapperX;
import com.zhush.system.entity.SysOpenApiLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 开放 API 调用日志 Mapper
 *
 * @author zhush
 */
@Mapper
public interface SysOpenApiLogMapper extends BaseMapperX<SysOpenApiLog> {
}
