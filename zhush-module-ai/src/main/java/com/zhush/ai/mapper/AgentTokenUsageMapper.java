package com.zhush.ai.mapper;

import com.zhush.ai.entity.AgentTokenUsageEntity;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 用量汇总 Mapper
 *
 * @author zhush
 */
@Mapper
public interface AgentTokenUsageMapper extends BaseMapperX<AgentTokenUsageEntity> {
}
