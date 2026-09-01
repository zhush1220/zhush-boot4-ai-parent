package com.zhush.system.vo;

import com.zhush.system.entity.SysNotice;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知公告详情 / 列表返回
 *
 * @author zhush
 */
@Data
@AutoMapper(target = SysNotice.class)
@Schema(description = "通知公告详情 / 列表返回")
public class AdminNoticeVO {

    @Schema(description = "公告ID")
    private Long id;

    @Schema(description = "公告标题")
    private String title;

    @Schema(description = "公告类型：1-通知 2-公告 3-警示")
    private Integer type;

    @Schema(description = "状态：0-草稿 1-已发布 2-已撤回")
    private Integer status;

    @Schema(description = "富文本HTML内容")
    private String content;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
