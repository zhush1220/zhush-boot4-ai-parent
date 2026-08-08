package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionProductSyncLogQuery
 * @Description 商品同步日志表 分页查询请求
 * @Author zhush
 * @Date 2026/08/08
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "商品同步日志表分页查询请求")
public class UnionProductSyncLogQuery extends BaseQuery {

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

    @Schema(description = "0-进行中 1-成功 2-失败")
    private Integer status;

}
