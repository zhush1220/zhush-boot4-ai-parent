package com.zhush.union.vo;

import com.zhush.union.entity.UnionProductSyncLog;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionProductSyncLogVO
 * @Description 商品同步日志表 详情/列表返回
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@AutoMapper(target = UnionProductSyncLog.class)
@Schema(description = "商品同步日志表详情/列表返回")
public class UnionProductSyncLogVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "平台")
    private Integer platform;

    @Schema(description = "同步类型：1-全量 2-增量 3-手动")
    private Integer syncType;

    @Schema(description = "同步总数")
    private Integer totalCount;

    @Schema(description = "成功数")
    private Integer successCount;

    @Schema(description = "失败数")
    private Integer failCount;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "start_time")
    private LocalDateTime startTime;

    @Schema(description = "end_time")
    private LocalDateTime endTime;

    @Schema(description = "0-进行中 1-成功 2-失败")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
