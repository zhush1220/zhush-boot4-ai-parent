package com.zhush.ai.mapper;

import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.ai.entity.AiChatMessageEntity;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 对话消息 Mapper
 *
 * @author zhush
 */
@Mapper
public interface AiChatMessageMapper extends BaseMapperX<AiChatMessageEntity> {

    /**
     * 查询会话内消息（按序号升序）。
     */
    default java.util.List<AiChatMessageEntity> selectBySessionId(String sessionId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(AiChatMessageEntity::getSessionId).eq(sessionId)
                .orderBy(AiChatMessageEntity::getSeq, true);
        return selectListByQuery(queryWrapper);
    }
}
