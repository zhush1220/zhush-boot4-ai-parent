package com.zhush.system.mapper;

import com.zhush.mybatis.core.mapper.BaseMapperX;
import com.zhush.system.entity.SysSqlAuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * SQL 审计日志 Mapper
 *
 * @author zhush
 */
@Mapper
public interface SysSqlAuditLogMapper extends BaseMapperX<SysSqlAuditLog> {
}
