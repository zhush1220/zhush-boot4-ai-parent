package com.zhush.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告查看记录返回
 *
 * @author zhush
 */
@Data
@Schema(description = "公告查看记录返回")
public class NoticeRecordVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "公告ID")
    private Long noticeId;

    @Schema(description = "查看人ID")
    private Long userId;

    @Schema(description = "查看人姓名")
    private String userName;

    @Schema(description = "最近查看时间")
    private LocalDateTime viewTime;

    @Schema(description = "首次查看时间")
    private LocalDateTime createTime;
}
