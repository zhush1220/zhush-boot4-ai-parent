package com.zhush.system.param;

import com.zhush.system.entity.SysNotice;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知公告新增/修改请求参数
 *
 * @author zhush
 */
@Data
@AutoMapper(target = SysNotice.class)
@Schema(description = "通知公告新增/修改请求参数")
public class AdminNoticeParam {

    @Schema(description = "公告ID（新增时不需要传）")
    private Long id;

    @Schema(description = "公告标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "公告标题不能为空")
    private String title;

    @Schema(description = "公告类型：1-通知 2-公告 3-警示", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "公告类型不能为空")
    private Integer type;

    @Schema(description = "状态：0-草稿 1-已发布 2-已撤回（不传默认草稿）")
    private Integer status;

    @Schema(description = "富文本HTML内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "公告内容不能为空")
    private String content;

    @Schema(description = "发布时间（status=1 时写入，可空由服务端赋值）")
    private LocalDateTime publishTime;
}
