package com.zhush.ai.mapper;

import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.ai.entity.AiSensitiveWordEntity;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * AI 敏感词库 Mapper
 *
 * @author zhush
 */
@Mapper
public interface AiSensitiveWordMapper extends BaseMapperX<AiSensitiveWordEntity> {

    /**
     * 按敏感词查询（唯一）。
     */
    default AiSensitiveWordEntity selectByWord(String word) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(AiSensitiveWordEntity::getWord).eq(word);
        return selectOneByQuery(queryWrapper);
    }

    /**
     * 启用态敏感词列表。
     */
    default List<AiSensitiveWordEntity> selectEnabled() {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(AiSensitiveWordEntity::getStatus).eq(1);
        return selectListByQuery(queryWrapper);
    }
}
