package com.zhush.ai.mapper;

import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.ai.entity.AiModelEntity;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * AI 模型配置 Mapper
 *
 * @author zhush
 */
@Mapper
public interface AiModelMapper extends BaseMapperX<AiModelEntity> {

    /**
     * 按编码查询（唯一）。
     */
    default AiModelEntity selectByCode(String code) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(AiModelEntity::getCode).eq(code);
        return selectOneByQuery(queryWrapper);
    }

    /**
     * 启用态模型列表（按优先级倒序）。
     */
    default List<AiModelEntity> selectEnabled() {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(AiModelEntity::getStatus).eq(1);
        queryWrapper.orderBy(AiModelEntity::getPriority, false);
        return selectListByQuery(queryWrapper);
    }

    /**
     * 当前默认模型。
     */
    default AiModelEntity selectDefault() {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(AiModelEntity::getIsDefault).eq(1);
        return selectOneByQuery(queryWrapper);
    }
}
