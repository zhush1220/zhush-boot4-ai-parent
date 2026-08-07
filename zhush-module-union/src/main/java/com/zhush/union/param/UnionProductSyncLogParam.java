package com.zhush.union.param;

import com.zhush.union.entity.UnionProductSyncLog;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionProductSyncLogParam
 * @Description 商品同步日志表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@AutoMapper(target = UnionProductSyncLog.class)
@Schema(description = "商品同步日志表新增/修改请求参数")
public class UnionProductSyncLogParam {

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

}
