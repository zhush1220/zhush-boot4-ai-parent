package com.zhush.system.entity;

import com.mybatisflex.annotation.Table;
import com.zhush.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知公告
 *
 * @author zhush
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_notice")
public class SysNotice extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 公告标题 */
    private String title;

    /** 公告类型：1-通知 2-公告 3-警示 */
    private Integer type;

    /** 状态：0-草稿 1-已发布 2-已撤回 */
    private Integer status;

    /** 富文本HTML内容 */
    private String content;

    /** 发布时间（status=1 时写入） */
    private LocalDateTime publishTime;
}
