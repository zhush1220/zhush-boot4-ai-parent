package com.zhush.ai.mapper;

import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.ai.entity.AgentDefinitionEntity;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

/**
 * 智能体定义 Mapper
 *
 * @author zhush
 */
@Mapper
public interface AgentDefinitionMapper extends BaseMapperX<AgentDefinitionEntity> {

    /**
     * 按唯一标识查询启用状态的定义。
     */
    default AgentDefinitionEntity selectByName(String name) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(AgentDefinitionEntity::getName).eq(name);
        return selectOneByQuery(queryWrapper);
    }
}
