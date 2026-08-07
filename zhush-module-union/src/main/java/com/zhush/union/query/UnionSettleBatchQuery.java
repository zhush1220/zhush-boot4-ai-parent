package com.zhush.union.query;

import com.zhush.common.query.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName UnionSettleBatchQuery
 * @Description 结算批次表 分页查询请求
 * @Author zhush
 * @Date 2026/08/07
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "结算批次表分页查询请求")
public class UnionSettleBatchQuery extends BaseQuery {

    @Schema(description = "批次号（业务唯一）")
    private String batchNo;

    @Schema(description = "包含记录数")
    private Integer recordCount;

    @Schema(description = "状态：1-待审核 2-审核通过 3-已结算 4-已驳回")
    private Integer status;

    @Schema(description = "操作员ID")
    private Long operatorId;

    @Schema(description = "remark")
    private String remark;

}
