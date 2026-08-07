package com.zhush.union.param;

import com.zhush.union.entity.UnionSettleBatch;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @ClassName UnionSettleBatchParam
 * @Description 结算批次表 新增/修改请求参数
 * @Author zhush
 * @Date 2026/08/07
 **/
@Data
@AutoMapper(target = UnionSettleBatch.class)
@Schema(description = "结算批次表新增/修改请求参数")
public class UnionSettleBatchParam {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "批次号（业务唯一）")
    private String batchNo;

    @Schema(description = "批次总金额")
    private BigDecimal totalAmount;

    @Schema(description = "包含记录数")
    private Integer recordCount;

    @Schema(description = "状态：1-待审核 2-审核通过 3-已结算 4-已驳回")
    private Integer status;

    @Schema(description = "操作员ID")
    private Long operatorId;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "结算完成时间")
    private LocalDateTime settleTime;

    @Schema(description = "remark")
    private String remark;

}
