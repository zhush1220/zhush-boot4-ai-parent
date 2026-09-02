package com.zhush.system.mapper;

import com.zhush.mybatis.core.mapper.BaseMapperX;
import com.zhush.system.entity.SysJobLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定时任务执行记录 Mapper
 *
 * @author zhush
 */
@Mapper
public interface SysJobLogMapper extends BaseMapperX<SysJobLog> {
}
