package com.zhush.system.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 通知公告分页查询请求
 *
 * @author zhush
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "通知公告分页查询请求")
public class AdminNoticeQuery extends BaseQuery {

    @Schema(description = "公告标题（模糊匹配）")
    private String title;

    @Schema(description = "公告类型：1-通知 2-公告 3-警示")
    private Integer type;

    @Schema(description = "状态：0-草稿 1-已发布 2-已撤回")
    private Integer status;

    @Schema(description = "创建时间起（yyyy-MM-dd HH:mm:ss）")
    private LocalDateTime createTimeBegin;

    @Schema(description = "创建时间止（yyyy-MM-dd HH:mm:ss）")
    private LocalDateTime createTimeEnd;
}
