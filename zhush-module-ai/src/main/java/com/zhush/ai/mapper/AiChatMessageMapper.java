package com.zhush.ai.mapper;

import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.ai.entity.AiChatMessageEntity;
import com.zhush.ai.vo.SessionTaskStatVO;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

    /**
     * 会话批量任务统计（任务类型集合 + 产物消息数）：供会话记录列表展示，一次聚合避免 N+1。
     */
    @Select("""
            <script>
            SELECT session_id AS sessionId,
                   GROUP_CONCAT(DISTINCT task_type) AS taskTypes,
                   SUM(CASE WHEN attachments IS NOT NULL AND attachments NOT IN ('', '[]') THEN 1 ELSE 0 END) AS attachmentCount
              FROM ai_chat_message
             WHERE session_id IN
            <foreach collection="sessionIds" item="sid" open="(" separator="," close=")">#{sid}</foreach>
             GROUP BY session_id
            </script>
            """)
    java.util.List<SessionTaskStatVO> selectTaskStatsBySessionIds(@Param("sessionIds") java.util.Collection<String> sessionIds);
}
