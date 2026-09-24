package com.zhush.ai.mapper;

import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.ai.entity.AiGenerationTaskEntity;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 生成任务 Mapper
 *
 * @author zhush
 */
@Mapper
public interface AiGenerationTaskMapper extends BaseMapperX<AiGenerationTaskEntity> {

    /**
     * 按对外任务号查询（前端轮询 / 越权校验用）。
     */
    default AiGenerationTaskEntity selectByTaskId(String taskId) {
        return selectOneByQuery(QueryWrapper.create()
                .where(AiGenerationTaskEntity::getTaskId).eq(taskId));
    }

    /**
     * 兜底扫描：未完成（排队/运行）且提交时间早于阈值的历史任务，交由定时线程主动查询收敛。
     */
    default List<AiGenerationTaskEntity> selectStaleUnfinished(LocalDateTime before, int limit) {
        return selectListByQuery(QueryWrapper.create()
                .where(AiGenerationTaskEntity::getStatus).in(
                        AiGenerationTaskEntity.STATUS_QUEUED, AiGenerationTaskEntity.STATUS_RUNNING)
                .and(AiGenerationTaskEntity::getSubmitTime).le(before)
                .orderBy(AiGenerationTaskEntity::getSubmitTime, true)
                .limit(limit));
    }
}
