package com.zhush.system.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公告查看记录（每用户每公告一条，重复查看刷新 view_time）
 *
 * @author zhush
 */
@Data
@Table("sys_notice_record")
public class SysNoticeRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花ID主键 */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /** 公告ID */
    private Long noticeId;

    /** 查看人ID */
    private Long userId;

    /** 查看人姓名 */
    private String userName;

    /** 最近查看时间 */
    private LocalDateTime viewTime;

    /** 首次查看时间 */
    private LocalDateTime createTime;
}
