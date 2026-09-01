package com.zhush.system.mapper;

import com.mybatisflex.core.query.QueryWrapper;
import com.zhush.mybatis.core.mapper.BaseMapperX;
import com.zhush.system.entity.SysNoticeRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 公告查看记录 Mapper
 *
 * @author zhush
 */
@Mapper
public interface SysNoticeRecordMapper extends BaseMapperX<SysNoticeRecord> {

    /**
     * 按公告ID + 用户ID查询记录（去重 upsert 判断用）
     */
    default SysNoticeRecord selectByNoticeIdAndUserId(Long noticeId, Long userId) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysNoticeRecord::getNoticeId).eq(noticeId)
                .where(SysNoticeRecord::getUserId).eq(userId);
        return selectOneByQuery(queryWrapper);
    }

    /**
     * 按公告ID删除全部查看记录（删除公告时清理由用）
     */
    default void deleteByNoticeId(Long noticeId) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(SysNoticeRecord::getNoticeId).eq(noticeId);
        deleteByQuery(queryWrapper);
    }
}
