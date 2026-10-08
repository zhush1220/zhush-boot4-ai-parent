package com.zhush.ai.mapper;

import com.zhush.ai.entity.AiJevDecisionLogEntity;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

/**
 * Jev 决策记录 Mapper（观测数据面：只写不读，直查走 SQL 报表）
 *
 * @author zhush
 */
@Mapper
public interface AiJevDecisionLogMapper extends BaseMapperX<AiJevDecisionLogEntity> {
}
